package com.nickmous.beanstash.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.controller.dto.auth.LoginRequest;
import com.nickmous.beanstash.controller.dto.auth.RegisterRequest;
import com.nickmous.beanstash.controller.dto.auth.VerifyTotpRequest;
import com.nickmous.beanstash.domain.security.totp.Totp;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TotpAuthenticationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    private static final String USERNAME = "loginuser";
    private static final String PASSWORD = "securepassword1";

    @BeforeEach
    void setUp() throws Exception {
        User existingUser = userRepository.findByUsername(USERNAME);
        if (existingUser != null) {
            userRepository.delete(existingUser);
        }

        var registerRequest = new RegisterRequest(USERNAME, "login@example.com", PASSWORD, "Login", "User");
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        User user = userRepository.findByUsername(USERNAME);
        String validCode = Totp.generateCode(user.getTotpSecret(), Instant.now());

        var verifyRequest = new VerifyTotpRequest(USERNAME, validCode);
        mockMvc.perform(post("/api/v1/auth/register/verify-totp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
            .andExpect(status().isOk());
    }

    @Test
    void login_withValidPasswordAndTotp_returns200() throws Exception {
        User user = userRepository.findByUsername(USERNAME);
        String totpCode = Totp.generateCode(user.getTotpSecret(), Instant.now());

        var loginRequest = new LoginRequest(USERNAME, PASSWORD, totpCode);

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk());
    }

    @Test
    void login_withValidPasswordButMissingTotp_returns401() throws Exception {
        var loginRequest = new LoginRequest(USERNAME, PASSWORD, null);

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withValidPasswordButWrongTotp_returns401() throws Exception {
        var loginRequest = new LoginRequest(USERNAME, PASSWORD, "000000");

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        User user = userRepository.findByUsername(USERNAME);
        String totpCode = Totp.generateCode(user.getTotpSecret(), Instant.now());

        var loginRequest = new LoginRequest(USERNAME, "wrongpassword1", totpCode);

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
    }
}
