package com.nickmous.beanstash.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nickmous.beanstash.controller.HomeController;
import com.nickmous.beanstash.domain.security.CustomUserDetailsService;
import com.nickmous.beanstash.domain.security.RoleService;
import com.nickmous.beanstash.domain.security.passkey.PasskeyRegistrationService;
import com.nickmous.beanstash.domain.security.totp.TotpService;
import com.nickmous.beanstash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureRestTestClient
@WebMvcTest(HomeController.class)
@Import(MethodSecurityTests.TestMethodSecurityConfig.class)
public class MethodSecurityTests {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestMethodSecurityConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TotpService totpService;

    @MockitoBean
    private PasskeyRegistrationService passkeyRegistrationService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void authenticatedUserWithRequiredAuthority_returns200() throws Exception {
        mockMvc.perform(get("/")
                .with(SecurityMockMvcRequestPostProcessors.user("testuser")
                    .authorities(new SimpleGrantedAuthority("package:read"))))
            .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserWithoutRequiredAuthority_returns403() throws Exception {
        mockMvc.perform(get("/")
                .with(SecurityMockMvcRequestPostProcessors.user("testuser")
                    .authorities(new SimpleGrantedAuthority("other:permission"))))
            .andExpect(status().isForbidden());
    }
}
