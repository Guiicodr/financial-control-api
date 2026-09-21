package com.guilherme.controlefinanceiro.config;

import com.guilherme.controlefinanceiro.repository.PasswordResetTokenRepository;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Expurgo periódico das credenciais vencidas (LGPD arts. 15 e 16).
 *
 * O código já recusava token vencido na validação, mas o registro continuava no
 * banco indefinidamente: dado pessoal de autenticação sem finalidade, que só
 * aumenta o impacto de um vazamento. Rodar de madrugada evita concorrência com
 * o uso normal e o job é idempotente (rodar duas vezes não muda o resultado).
 */
@Component
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;

    public TokenCleanupJob(RefreshTokenRepository refreshTokens, PasswordResetTokenRepository resetTokens) {
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
    }

    /** Cron configurável por RETENTION_TOKEN_CLEANUP_CRON (padrão: diário às 03:30). */
    @Scheduled(cron = "${app.retention.token-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void expurgarTokensVencidos() {
        Instant agora = Instant.now();
        long vencidosRefresh = refreshTokens.countByExpiracaoBefore(agora);
        long vencidosReset = resetTokens.countByExpiracaoBefore(agora);

        if (vencidosRefresh == 0 && vencidosReset == 0) {
            log.debug("Expurgo de retenção: nenhuma credencial vencida para remover.");
            return;
        }

        if (vencidosRefresh > 0)
            refreshTokens.deleteByExpiracaoBefore(agora);
        if (vencidosReset > 0)
            resetTokens.deleteByExpiracaoBefore(agora);

        log.info("Expurgo de retenção: {} refresh token(s) e {} token(s) de recuperação vencidos removidos.",
                vencidosRefresh, vencidosReset);
    }
}
