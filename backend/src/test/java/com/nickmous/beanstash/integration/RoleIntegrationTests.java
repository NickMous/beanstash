package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
public class RoleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registeredUser_getsDefaultRoleWithoutDirectAuthorities() throws Exception {
        var registerRequest = new RegisterRequest("roleuser", "roleuser@example.com", "securepassword1", "Role", "User");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        User user = userRepository.findByUsername("roleuser");
        assertThat(user.getAuthorities()).isEmpty();
        assertThat(user.getRoles())
            .extracting(role -> role.getName())
            .containsExactly("user");
    }

    @Test
    void roleDerivedAuthority_grantsAccessToSecuredEndpoint() throws Exception {
        var registerRequest = new RegisterRequest("roleaccess", "roleaccess@example.com", "securepassword1", "Role", "Access");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isOk());

        User user = userRepository.findByUsername("roleaccess");
        String code = Totp.generateCode(user.getTotpSecret(), Instant.now());

        mockMvc.perform(post("/api/v1/auth/register/verify-totp")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new VerifyTotpRequest("roleaccess", code))))
            .andExpect(status().isOk());

        user = userRepository.findByUsername("roleaccess");
        String loginCode = Totp.generateCode(user.getTotpSecret(), Instant.now());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("roleaccess", "securepassword1", loginCode))))
            .andExpect(status().isOk())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        SecurityContext securityContext = (SecurityContext) session
            .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext.getAuthentication().getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .contains("package:read");

        mockMvc.perform(get("/api/v1/").session(session))
            .andExpect(status().isOk());
    }
}
