package com.guilherme.controlefinanceiro.config;

import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    /** Trava extra: o usuário de teste só é criado se explicitamente liberado. */
    private final boolean seedTestUser;

    public DataInitializer(UsuarioRepository repository,
            PasswordEncoder encoder,
            @Value("${app.dev.seed-test-user:true}") boolean seedTestUser) {
        this.repository = repository;
        this.encoder = encoder;
        this.seedTestUser = seedTestUser;
    }

    @Override
    public void run(String... args) {
        if (!seedTestUser) {
            log.info("Seed do usuário de teste desabilitado (app.dev.seed-test-user=false).");
            return;
        }
        String email = "teste@teste.com";
        if (repository.findByEmail(email).isEmpty()) {
            var usuario = new Usuario("Usuário Teste", email, encoder.encode("teste123"));
            repository.save(usuario);
            log.info("✅ Test user created: {}", email);
        } else {
            log.info("ℹ️ Test user already exists: {}", email);
        }
    }
}