package com.nickmous.beanstash.functional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickmous.beanstash.controller.AuthController;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@WebAppConfiguration
public class AuthControllerTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mvc;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @Transactional
    public void loginPost_ShouldReturn200() throws Exception {
        // Create test user with encoded password
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("testpass123"));
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(testUser);

        // Test user credentials
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("testpass123");

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk());
    }

    @Test
    public void loginPost_ShouldReturn403_ForInvalidCredentials() throws Exception {
        // Test invalid user credentials
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("wrongpassword");

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    public void loginPost_ShouldReturn403_ForWrongPassword() throws Exception {
        // Create test user with encoded password
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("testpass123"));
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(testUser);

        // Test wrong password
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");
        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    public void loginPost_ShouldReturn403_ForInactiveUser() throws Exception {
        // Create inactive test user
        User testUser = new User();
        testUser.setUsername("inactiveuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("testpass123"));
        testUser.setActive(false); // Inactive user
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(testUser);

        // Test inactive user credentials
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("inactiveuser");
        loginRequest.setPassword("testpass123");
        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    public void loginPost_ShouldReturn403_ForDeletedUser() throws Exception {
        // Create deleted test user
        User testUser = new User();
        testUser.setUsername("deleteduser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("testpass123"));
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setDeletedAt(LocalDateTime.now()); // Soft deleted user
        userRepository.save(testUser);

        // Test deleted user credentials
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("deleteduser");
        loginRequest.setPassword("testpass123");
        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    public void loginPost_returnsJwtToken_OnSuccessfulLogin() throws Exception {
        // Create test user with encoded password
        User testUser = new User();
        testUser.setUsername("jwtuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("jwtpass123"));
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(testUser);

        // Test user credentials
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("jwtuser");
        loginRequest.setPassword("jwtpass123");
        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    AuthController.AuthResponse authResponse = objectMapper.readValue(responseBody, AuthController.AuthResponse.class);
                    assertThat(authResponse.getToken()).isNotNull().isNotEmpty();
                });
    }
}