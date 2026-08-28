package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.controller.dto.auth.RegisterRequest;
import com.nickmous.beanstash.controller.dto.auth.VerifyTotpRequest;
import com.nickmous.beanstash.domain.security.totp.Totp;
import com.nickmous.beanstash.entity.Authority;
import com.nickmous.beanstash.entity.Role;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.time.Instant;
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
public class RegistrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Test
    void register_withValidData_returnsTotpSetup() throws Exception {
        var request = new RegisterRequest("newuser", "new@example.com", "securepassword1", "New", "User");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.secret").isNotEmpty())
            .andExpect(jsonPath("$.otpAuthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")));
    }

    @Test
    void register_withDuplicateUsername_returns409() throws Exception {
        var request = new RegisterRequest("dupuser", "dup@example.com", "securepassword1", "Dup", "User");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void register_withMissingFields_returns400() throws Exception {
        String body = """
            {"username": "", "email": "bad"}
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        var request = new RegisterRequest("shortpw", "short@example.com", "short", "Short", "Pw");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyTotp_withValidCode_returns200() throws Exception {
        var registerRequest = new RegisterRequest("verifyuser", "verify@example.com", "securepassword1", "Verify", "User");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        User user = userRepository.findByUsername("verifyuser");
        String validCode = Totp.generateCode(user.getTotpSecret(), Instant.now());

        var verifyRequest = new VerifyTotpRequest("verifyuser", validCode);

        mockMvc.perform(post("/api/v1/auth/register/verify-totp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
            .andExpect(status().isOk());

        User updatedUser = userRepository.findByUsername("verifyuser");
        assertThat(updatedUser.isTotpEnabled()).isTrue();
    }

    @Test
    void register_withValidData_assignsDefaultRole() throws Exception {
        var request = new RegisterRequest("authdefaultuser", "authdefault@example.com", "securepassword1", "Auth", "Default");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        User user = userRepository.findByUsername("authdefaultuser");
        assertThat(user.getRoles()).hasSize(1);
        Role defaultRole = user.getRoles().iterator().next();
        assertThat(defaultRole.getName()).isEqualTo("user");
        assertThat(defaultRole.getAuthorities())
            .extracting(Authority::getName)
            .contains("package:read");
    }

    @Test
    void verifyTotp_withUnknownUsername_returns400() throws Exception {
        var verifyRequest = new VerifyTotpRequest("nosuchuser", "000000");

        mockMvc.perform(post("/api/v1/auth/register/verify-totp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyTotp_withInvalidCode_returns400() throws Exception {
        var registerRequest = new RegisterRequest("badcodeuser", "badcode@example.com", "securepassword1", "Bad", "Code");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        var verifyRequest = new VerifyTotpRequest("badcodeuser", "000000");

        mockMvc.perform(post("/api/v1/auth/register/verify-totp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
            .andExpect(status().isBadRequest());
    }
}
