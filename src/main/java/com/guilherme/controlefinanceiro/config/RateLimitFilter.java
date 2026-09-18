package com.guilherme.controlefinanceiro.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit em memória (janela fixa) para os endpoints que não passam por JWT.
 *
 * Motivação de segurança:
 * <ul>
 *   <li>{@code /auth/login} — sem limite, dá para testar senhas em massa
 *       (brute force) contra o BCrypt e, na prática, sondar contas.</li>
 *   <li>{@code /auth/forgot-password} — era um vetor de mail bombing.</li>
 *   <li>{@code /webhooks/**} — endpoint público que grava lançamentos: limitar
 *       reduz o dano de um flood.</li>
 * </ul>
 *
 * Observação de escalabilidade: o contador é por instância (não distribuído).
 * Com mais de uma réplica o limite efetivo é {@code limite x réplicas} — o que
 * ainda resolve o abuso trivial. Se virar requisito forte, trocar o
 * {@link ConcurrentHashMap} por Redis (spring-boot-starter-data-redis).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Endpoints de autenticação sujeitos ao limite mais restrito. */
    private static final Set<String> AUTH_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password");

    private static final String WEBHOOK_PREFIX = "/webhooks/";

    /** Acima disso, varremos as janelas expiradas para o mapa não crescer sem limite. */
    private static final int TAMANHO_MAXIMO_MAPA = 10_000;

    private final int limiteAuth;
    private final int limiteWebhook;
    private final long janelaMs;
    private final Clock clock;

    private final ConcurrentHashMap<String, Contador> contadores = new ConcurrentHashMap<>();
    private final AtomicLong requisicoes = new AtomicLong();

    public RateLimitFilter(int limiteAuth, int limiteWebhook, long janelaMs) {
        this(limiteAuth, limiteWebhook, janelaMs, Clock.systemUTC());
    }

    /** Construtor com Clock injetável para os testes de janela/expiração. */
    public RateLimitFilter(int limiteAuth, int limiteWebhook, long janelaMs, Clock clock) {
        this.limiteAuth = limiteAuth;
        this.limiteWebhook = limiteWebhook;
        this.janelaMs = janelaMs;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return bucket(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String caminho = request.getRequestURI();
        String bucket = bucket(caminho);
        int limite = AUTH_PATHS.contains(caminho) ? limiteAuth : limiteWebhook;
        String cliente = chaveCliente(request);
        String chave = cliente + "|" + bucket;

        long agora = clock.millis();
        Contador contador = contadores.computeIfAbsent(chave, ignorado -> new Contador(agora));

        long restanteMs;
        boolean excedeu;
        synchronized (contador) {
            if (agora - contador.inicioJanela >= janelaMs) {
                contador.inicioJanela = agora;
                contador.quantidade = 0;
            }
            contador.quantidade++;
            excedeu = contador.quantidade > limite;
            restanteMs = Math.max(1, janelaMs - (agora - contador.inicioJanela));
        }

        if (excedeu) {
            long segundos = restanteMs / 1000 + 1;
            log.warn("Rate limit excedido em {} para {} (limite {}/{}s)",
                    bucket, cliente, limite, janelaMs / 1000);
            responder429(response, segundos);
            return;
        }

        limparSeNecessario(agora);
        filterChain.doFilter(request, response);
    }

    /** Define se o caminho é protegido e qual bucket usar (auth ou webhook). */
    private String bucket(String caminho) {
        if (AUTH_PATHS.contains(caminho)) {
            return "auth";
        }
        if (caminho != null && caminho.startsWith(WEBHOOK_PREFIX)) {
            return "webhook";
        }
        return null;
    }

    /**
     * A app roda atrás do proxy do Railway, então {@code getRemoteAddr()} seria
     * sempre o IP do proxy (todos os usuários na mesma cota). O primeiro valor
     * de {@code X-Forwarded-For} é o IP original do cliente.
     */
    private String chaveCliente(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            int virgula = encaminhado.indexOf(',');
            String primeiro = (virgula > 0 ? encaminhado.substring(0, virgula) : encaminhado).trim();
            if (!primeiro.isEmpty()) {
                return primeiro;
            }
        }
        return request.getRemoteAddr();
    }

    private void responder429(HttpServletResponse response, long segundos) throws IOException {
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"error\":\"Muitas tentativas. Aguarde " + segundos + " segundos e tente novamente.\"}");
    }

    private void limparSeNecessario(long agora) {
        if (requisicoes.incrementAndGet() % 500 != 0 || contadores.size() < TAMANHO_MAXIMO_MAPA) {
            return;
        }
        contadores.entrySet().removeIf(entrada -> agora - entrada.getValue().inicioJanela >= janelaMs);
    }

    /** Janela fixa simples e mutável, sincronizada por instância. */
    private static final class Contador {
        private long inicioJanela;
        private int quantidade;

        private Contador(long inicioJanela) {
            this.inicioJanela = inicioJanela;
        }
    }
}