package com.nickmous.beanstash.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nickmous.beanstash.configuration.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The real CSRF round-trip the SPA performs: fetch the token from
 * GET /api/v1/auth/csrf, then send it back in the header on an unsafe request.
 * The {@code .with(csrf())} post-processor used in the other tests bypasses the
 * request handler, so it would not catch a token-masking mismatch between the
 * endpoint and the header.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class CsrfTokenFlowTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BOGUS_LOGIN =
        "{\"username\":\"nobody\",\"password\":\"wrongpassword1\",\"totpCode\":null}";

    @Test
    void tokenFromCsrfEndpoint_sentAsHeader_passesCsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(csrf.getResponse().getContentAsString());
        String headerName = body.get("headerName").asText();
        String token = body.get("token").asText();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession();

        // 401 (bad credentials), not 403: the CSRF filter accepted the token.
        mockMvc.perform(post("/api/v1/auth/login")
                .session(session)
                .header(headerName, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOGUS_LOGIN))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unsafeRequestWithoutToken_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOGUS_LOGIN))
            .andExpect(status().isForbidden());
    }

    @Test
    void logout_invalidatesTheToken() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(csrf.getResponse().getContentAsString());
        String headerName = body.get("headerName").asText();
        String token = body.get("token").asText();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession();

        mockMvc.perform(post("/logout").session(session).header(headerName, token))
            .andExpect(status().isNoContent());

        // Same token, now-dead session: the CSRF check fails, so the SPA must re-fetch.
        mockMvc.perform(post("/api/v1/auth/login")
                .session(session)
                .header(headerName, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOGUS_LOGIN))
            .andExpect(status().isForbidden());
    }

    @Test
    void unsafeRequestWithGarbageToken_isForbidden() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) csrf.getRequest().getSession();

        mockMvc.perform(post("/api/v1/auth/login")
                .session(session)
                .header("X-XSRF-TOKEN", "not-the-real-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOGUS_LOGIN))
            .andExpect(status().isForbidden());
    }
}
