package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.domain.security.passkey.PasskeyRegistrationService;
import com.nickmous.beanstash.domain.security.passkey.PendingPasskeyRegistration;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.registration.HttpSessionPublicKeyCredentialCreationOptionsRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the completion half of the passkey signup flow (POST /register/passkey). The
 * credential verification itself needs a real authenticator, so {@link PasskeyRegistrationService}
 * is mocked here; what these tests pin down is the controller's session contract: pending state
 * required, cleared on success, and the new account left authenticated.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class PasskeyRegistrationCompletionTests {

    // Must match AuthController.PENDING_PASSKEY_REGISTRATION_ATTR.
    private static final String PENDING_ATTR = "com.nickmous.beanstash.passkey.PENDING_REGISTRATION";

    // Structurally valid RelyingPartyPublicKey body (same shape the front-end sends); the
    // base64url payloads are dummies since credential verification happens in the mocked service.
    private static final String CREDENTIAL_JSON = """
        {
          "credential": {
            "id": "AQIDBA",
            "rawId": "AQIDBA",
            "type": "public-key",
            "response": {
              "clientDataJSON": "AQIDBA",
              "attestationObject": "AQIDBA",
              "transports": ["internal"]
            },
            "authenticatorAttachment": "platform"
          },
          "label": "test passkey"
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private PasskeyRegistrationService passkeyRegistrationService;

    @Test
    void completePasskeyRegistration_withoutPendingRegistration_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/passkey")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREDENTIAL_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void completePasskeyRegistration_withPendingRegistration_authenticatesAndClearsPendingState() throws Exception {
        // Prepare the session exactly as the options endpoint would have left it.
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest sessionRequest = new MockHttpServletRequest();
        sessionRequest.setSession(session);
        new HttpSessionPublicKeyCredentialCreationOptionsRepository()
            .save(sessionRequest, new MockHttpServletResponse(), PublicKeyCredentialCreationOptions.builder().build());
        session.setAttribute(PENDING_ATTR,
            new PendingPasskeyRegistration("pkcomplete", "pkcomplete@example.com", "PK", "Complete"));

        // The real service persists the account inside completeRegistration; mirror that
        // contract so the controller can authenticate the new user afterwards.
        doAnswer(invocation -> {
            User user = new User();
            user.setUsername("pkcomplete");
            user.setEmail("pkcomplete@example.com");
            user.setFirstName("PK");
            user.setLastName("Complete");
            userRepository.save(user);
            return null;
        }).when(passkeyRegistrationService).completeRegistration(any(), any());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/passkey")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREDENTIAL_JSON))
            .andExpect(status().isOk())
            .andReturn();

        ArgumentCaptor<PendingPasskeyRegistration> pendingCaptor =
            ArgumentCaptor.forClass(PendingPasskeyRegistration.class);
        verify(passkeyRegistrationService).completeRegistration(any(), pendingCaptor.capture());
        assertThat(pendingCaptor.getValue().username()).isEqualTo("pkcomplete");
        assertThat(pendingCaptor.getValue().email()).isEqualTo("pkcomplete@example.com");

        HttpSession resultSession = result.getRequest().getSession(false);
        assertThat(resultSession).isNotNull();
        assertThat(resultSession.getAttribute(PENDING_ATTR)).isNull();

        SecurityContext securityContext = (SecurityContext) resultSession.getAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication().getName()).isEqualTo("pkcomplete");
    }
}
