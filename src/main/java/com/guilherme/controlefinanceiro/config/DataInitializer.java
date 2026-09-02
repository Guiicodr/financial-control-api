package com.guilherme.controlefinanceiro.config;

import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public DataInitializer(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        String email = "teste@teste.com";
        if (repository.findByEmail(email).isEmpty()) {
            var usuario = new Usuario("Usuário Teste", email, encoder.encode("teste123"));
            repository.save(usuario);
            log.info("✅ Test user created: {} / {}", email, "teste123");
        } else {
            log.info("ℹ️ Test user already exists: {}", email);
        }
    }
}