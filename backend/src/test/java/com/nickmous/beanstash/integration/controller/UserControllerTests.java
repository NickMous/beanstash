package com.nickmous.beanstash.integration.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void seed() {
        User alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setFirstName("Alice");
        alice.setLastName("Example");
        userRepository.save(alice);

        User bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setFirstName("Bob");
        bob.setLastName("Example");
        userRepository.save(bob);

        User sander = new User();
        sander.setUsername("sander");
        sander.setEmail("sander@example.com");
        sander.setFirstName("Sander");
        sander.setLastName("Example");
        sander.setDeletedAt(Instant.EPOCH); // This user is soft-deleted and should not appear in results.
        userRepository.save(sander);
    }

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void getUser_isPubliclyReadable_returns200WithUserData() throws Exception {
        // No authentication: the endpoint is intentionally public (anonymous gets user:read by default).
        mockMvc.perform(get("/api/v1/user/alice"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.firstName").value("Alice"))
            .andExpect(jsonPath("$.lastName").value("Example"));
    }

    @Test
    void getUser_whenNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/user/ghost"))
            .andExpect(status().isNotFound());
    }

    @Test
    void listUsers_isPubliclyReadable_returns200WithUserData() throws Exception {
        // No authentication: the endpoint is intentionally public (anonymous gets user:read by default).
        // The list returns every HUMAN user, and the shared test database also holds users created by
        // other integration tests, so assert by content rather than by position.
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.username == 'alice')].email").value(hasItem("alice@example.com")))
            .andExpect(jsonPath("$[?(@.username == 'alice')].firstName").value(hasItem("Alice")))
            .andExpect(jsonPath("$[?(@.username == 'bob')].email").value(hasItem("bob@example.com")))
            .andExpect(jsonPath("$[?(@.username == 'sander')]").doesNotExist()) // Soft-deleted user is not returned.
            .andExpect(jsonPath("$[?(@.username == 'system')]").doesNotExist()); // System sentinel is not returned.
    }

    @Test
    void updateUser_whenNotAuthenticated_returns403() throws Exception {
        // Anonymous request: it carries user:read (granted by SecurityConfig.anonymous) so it clears the
        // request matcher, but @PreAuthorize requires user:write -> 403. csrf() is required because CSRF is
        // enabled; without it the POST is rejected at the CSRF filter (also 403) before authorization runs.
        mockMvc.perform(post("/api/v1/user/alice")
                .with(csrf())
                .contentType("application/json")
                .content("{\"firstName\": \"AliceUpdated\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_whenAuthenticatedButWithoutAuthority_returns403() throws Exception {
        // Authenticated principal that gets user:read (via IpAuthorityFilter) but not user:write.
        mockMvc.perform(post("/api/v1/user/alice")
                .with(user("reader"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"firstName\": \"AliceUpdated\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_whenAuthenticatedButWithoutAuthorityButOwnsUser_returns200() throws Exception {
        // Authenticated principal that gets user:read (via IpAuthorityFilter) but not user:write, but is
        // updating their own user record. This is allowed by the @PreAuthorize expression.
        mockMvc.perform(post("/api/v1/user/alice")
                .with(user("alice"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\": \"alice@example.com\", \"firstName\": \"AliceUpdated\", \"lastName\": \"Example\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.firstName").value("AliceUpdated"));
    }

    @Test
    void updateUser_whenAuthenticatedWithWriteAuthority_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/user/alice")
                .with(user("editor").authorities(new SimpleGrantedAuthority("user:write")))
                .with(csrf())
                .contentType("application/json")
                .content("{\"email\": \"alice@example.com\", \"firstName\": \"AliceUpdated\", \"lastName\": \"Example\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.firstName").value("AliceUpdated"));
    }

    @Test
    void deleteUser_whenNotAuthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/user/alice")
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_whenAuthenticatedButWithoutAuthority_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/user/alice")
                .with(user("reader"))
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_whenAuthenticatedWithWriteAuthority_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/user/alice")
                .with(user("editor").authorities(new SimpleGrantedAuthority("user:write")))
                .with(csrf()))
            .andExpect(status().isNoContent());
    }
}
