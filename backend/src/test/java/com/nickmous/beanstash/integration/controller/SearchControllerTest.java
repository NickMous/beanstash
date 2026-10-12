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

import java.time.Instant;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SearchControllerTest {
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
    void searchEndpoint_withAlicInQuery_returnsAlice() throws Exception {
        mockMvc.perform(get("/api/v1/search?query=Alic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].results[0].title").value("Alice Example (@alice)"))
                .andExpect(jsonPath("$.results[0].results[0].slug").value("alice"));
    }

    @Test
    void searchEndpoint_with3CharactersInQuery_returnsError() throws Exception {
        mockMvc.perform(get("/api/v1/search?query=ali"))
                .andExpect(status().isBadRequest());
    }
}
