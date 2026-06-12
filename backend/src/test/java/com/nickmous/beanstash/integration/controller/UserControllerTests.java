package com.nickmous.beanstash.integration.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
}
