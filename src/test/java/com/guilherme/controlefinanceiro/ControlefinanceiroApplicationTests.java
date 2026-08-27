package com.guilherme.controlefinanceiro;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.guilherme.controlefinanceiro.model.Usuario;
import com.guilherme.controlefinanceiro.repository.UsuarioRepository;
import com.guilherme.controlefinanceiro.repository.RefreshTokenRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControlefinanceiroApplicationTests {

	private static final String VERCEL_ORIGIN = "https://financial-control-dashboard-smoky.vercel.app";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void limparUsuarios() {
		refreshTokens.deleteAll();
		usuarios.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void preflightLoginPermiteOrigemVercel() throws Exception {
		mockMvc.perform(options("/auth/login")
				.header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
						org.hamcrest.Matchers.containsString("POST")));
	}

	@Test
	void loginPermitePostDaOrigemVercel() throws Exception {
		usuarios.save(new Usuario("Guilherme", "guilherme@example.com", passwordEncoder.encode("senha-segura")));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.ORIGIN, VERCEL_ORIGIN)
				.content("{\"email\":\"guilherme@example.com\",\"senha\":\"senha-segura\"}"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, VERCEL_ORIGIN));
	}

}
