package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.model.Transacao;
import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.IncomeRepository;
import com.guilherme.controlefinanceiro.repository.TransacaoRepository;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Processa mensagens recebidas via WhatsApp: localiza o usuário pelo número
 * de telefone, interpreta o texto, registra a transação e devolve a mensagem
 * de confirmação que será enviada de volta para o chat.
 */
@Service
public class WhatsAppService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final UsuarioRepository usuarios;
    private final TransacaoRepository transacoes;
    private final IncomeRepository incomes;

    public WhatsAppService(UsuarioRepository usuarios, TransacaoRepository transacoes, IncomeRepository incomes) {
        this.usuarios = usuarios;
        this.transacoes = transacoes;
        this.incomes = incomes;
    }

    /**
     * Processa a mensagem de um número e devolve o texto de resposta
     * (confirmação, erro ou instruções de vinculação).
     */
    @Transactional
    public String processarMensagem(String telefoneBruto, String mensagem) {
        String telefone = normalizarTelefone(telefoneBruto);
        if (telefone == null || telefone.isBlank())
            return "Não foi possível identificar seu número. Tente novamente mais tarde.";

        Optional<Usuario> usuarioOpt = usuarios.findByTelefone(telefone);
        if (usuarioOpt.isEmpty()) {
            return "Olá! Este número ainda não está vinculado a uma conta Finanly.\n"
                    + "Entre no app, abra *Perfil > WhatsApp* e informe este número para começar a registrar seus gastos por aqui.";
        }

        Usuario usuario = usuarioOpt.get();

        String comando = mensagem == null ? "" : mensagem.trim().toLowerCase(Locale.ROOT);
        if (comando.equals("saldo")) {
            return respostaSaldo(usuario);
        }
        if (comando.equals("ajuda") || comando.equals("help") || comando.equals("menu")) {
            return """
                    *Finanly — Lançamentos por WhatsApp*

                    Envie uma mensagem como:
                    • _gastei 25,50 no almoço_
                    • _uber 30_
                    • _recebi 3000 de salario_

                    Outros comandos:
                    • *saldo* — consulta seu saldo
                    • *ajuda* — mostra esta mensagem""";
        }

        WhatsAppMessageParser.Parsed parsed = WhatsAppMessageParser.parse(mensagem);
        if (parsed == null) {
            return "Não entendi o valor dessa mensagem. Envie algo como: _gastei 25,50 no almoço_ (ou *ajuda* para ver exemplos).";
        }

        Transacao transacao = new Transacao();
        transacao.setUsuario(usuario);
        transacao.setTipo(parsed.tipo());
        transacao.setValor(parsed.valor().doubleValue());
        transacao.setCategoria(parsed.categoria());
        transacao.setDescricao(parsed.descricao());
        transacao.setData(LocalDate.now());
        transacoes.save(transacao);

        log.info("Lançamento via WhatsApp: usuario={} valor={} tipo={} categoria={}",
                usuario.getEmail(), parsed.valor(), parsed.tipo(), parsed.categoria());

        return confirmacao(transacao, parsed.valor());
    }

    private String respostaSaldo(Usuario usuario) {
        double rendas = incomes.findAllByUsuario(usuario).stream()
                .filter(r -> r.getValor() != null)
                .mapToDouble(r -> r.getValor())
                .sum();

        double despesas = transacoes.findAllByUsuario(usuario).stream()
                .filter(t -> "SAIDA".equals(t.getTipo()) && t.getValor() != null)
                .mapToDouble(Transacao::getValor)
                .sum();

        return String.format(Locale.forLanguageTag("pt-BR"),
                "*Finanly* — Saldo atual: R$ %,.2f", rendas - despesas);
    }

    private String confirmacao(Transacao transacao, BigDecimal valor) {
        String rotulo = "ENTRADA".equals(transacao.getTipo()) ? "Entrada registrada" : "Gasto registrado";
        return String.format(Locale.forLanguageTag("pt-BR"),
                "✅ *%s*%nValor: R$ %,.2f%nCategoria: %s%nDescrição: %s%nData: %s%n%nDigite *saldo* para ver seu saldo ou *ajuda* para exemplos.",
                rotulo,
                valor,
                transacao.getCategoria() != null ? transacao.getCategoria().name() : "OUTROS",
                transacao.getDescricao() != null ? transacao.getDescricao() : "-",
                transacao.getData());
    }

    /**
     * Mantém apenas dígitos. Números do WhatsApp chegam com prefixo do país
     * (ex.: "whatsapp:+5511999998888" ou "+55 11 99999-8888").
     */
    public static String normalizarTelefone(String telefone) {
        if (telefone == null)
            return null;
        String digitos = telefone.replaceAll("\\D", "");
        return digitos.isBlank() ? null : digitos;
    }
}