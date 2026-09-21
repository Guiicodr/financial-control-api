package com.guilherme.controlefinanceiro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Necessario para o expurgo de retencao (TokenCleanupJob): sem @EnableScheduling
// o @Scheduled e aceito no boot e simplesmente nunca executa.
@EnableScheduling
public class ControlefinanceiroApplication {

	public static void main(String[] args) {
		SpringApplication.run(ControlefinanceiroApplication.class, args);
	}

}
