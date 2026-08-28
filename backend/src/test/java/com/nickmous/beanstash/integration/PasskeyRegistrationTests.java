package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.controller.dto.auth.PasskeyRegistrationOptionsRequest;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
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
public class PasskeyRegistrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Test
    void passkeyRegistrationOptions_withValidData_returnsCreationOptions() throws Exception {
        var request = new PasskeyRegistrationOptionsRequest(
            "passkeyuser", "passkey@example.com", "Passkey", "User");

        mockMvc.perform(post("/api/v1/auth/register/passkey/options")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.challenge").isNotEmpty())
            .andExpect(jsonPath("$.rp.id").value("localhost"))
            .andExpect(jsonPath("$.user.name").value("passkeyuser"));
    }

    @Test
    void passkeyRegistrationOptions_doesNotCreateUserUntilCeremonyCompletes() throws Exception {
        var request = new PasskeyRegistrationOptionsRequest(
            "pknopassword", "pknp@example.com", "PK", "NoPw");

        mockMvc.perform(post("/api/v1/auth/register/passkey/options")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        // Requesting options is preparatory: the account is created only once the ceremony
        // completes (POST /register/passkey), so nothing is persisted yet.
        assertThat(userRepository.findByUsername("pknopassword")).isNull();
    }

    @Test
    void passkeyRegistrationOptions_canBeRetriedForSameUsernameAfterAbandonedAttempt() throws Exception {
        var request = new PasskeyRegistrationOptionsRequest(
            "pkretry", "pkretry@example.com", "PK", "Retry");
        String body = objectMapper.writeValueAsString(request);

        // A first (abandoned) options request must not block a second one for the same username.
        mockMvc.perform(post("/api/v1/auth/register/passkey/options")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/register/passkey/options")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());
    }

    @Test
    void passkeyRegistrationOptions_withExistingAccount_returns409() throws Exception {
        User existing = new User();
        existing.setUsername("pkdupuser");
        existing.setEmail("pkdup@example.com");
        existing.setFirstName("Dup");
        existing.setLastName("User");
        userRepository.save(existing);

        var request = new PasskeyRegistrationOptionsRequest(
            "pkdupuser", "pkdup@example.com", "Dup", "User");

        mockMvc.perform(post("/api/v1/auth/register/passkey/options")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void passkeyRegistrationOptions_withMissingFields_returns400() throws Exception {
        String body = """
            {"username": ""}
            """;

        mockMvc.perform(post("/api/v1/auth/register/passkey/options")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void completePasskeyRegistration_withoutOptions_returns400() throws Exception {
        String body = """
            {"credential": {}, "label": "test"}
            """;

        mockMvc.perform(post("/api/v1/auth/register/passkey")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
