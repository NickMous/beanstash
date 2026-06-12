package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.controller.dto.LoginRequest;
import com.nickmous.beanstash.controller.dto.RegisterRequest;
import com.nickmous.beanstash.controller.dto.VerifyTotpRequest;
import com.nickmous.beanstash.domain.security.totp.Totp;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AuthorityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registeredUser_canAccessSecuredEndpoint() throws Exception {
        var registerRequest = new RegisterRequest("authflowuser", "authflow@example.com", "securepassword1", "Auth", "Flow");

        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        User user = userRepository.findByUsername("authflowuser");
        String code = Totp.generateCode(user.getTotpSecret(), Instant.now());

        mockMvc.perform(post("/auth/register/verify-totp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VerifyTotpRequest("authflowuser", code))))
            .andExpect(status().isOk());

        user = userRepository.findByUsername("authflowuser");
        String loginCode = Totp.generateCode(user.getTotpSecret(), Instant.now());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("authflowuser", "securepassword1", loginCode))))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        SecurityContext securityContext = (SecurityContext) session
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext.getAuthentication().getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .contains("package:read");

        mockMvc.perform(get("/").session(session))
            .andExpect(status().isOk());
    }
}
