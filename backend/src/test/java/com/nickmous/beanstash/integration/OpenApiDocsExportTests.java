package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nickmous.beanstash.configuration.TestcontainersConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards the committed {@code backend/openapi.json} against the live OpenAPI
 * specification that springdoc derives from the controllers. That file is the
 * contract the frontend module's openapi-generator plugin consumes, so it has
 * to stay in step with the code.
 *
 * <p>By default this asserts rather than writes, turning spec drift into a
 * failing build. When the API surface has legitimately changed, refresh the
 * file with {@code mvn verify -Dopenapi.write=true} and commit the result.</p>
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OpenApiDocsExportTests {

    private static final Path SPEC_PATH = Path.of(System.getProperty("user.dir"), "openapi.json");

    /** Set to {@code true} to overwrite the committed spec instead of asserting against it. */
    private static final String WRITE_PROPERTY = "openapi.write";

    @Autowired
    private MockMvc mockMvc;

    /*
     * springdoc builds the schema by reflection and does not guarantee a stable
     * property order between runs, so the raw document is not byte-comparable.
     * Sorting every object's keys canonicalises it — key order carries no
     * meaning in OpenAPI, and array order (required, tags, ...) is untouched.
     */
    private final ObjectMapper objectMapper = JsonMapper.builder()
        .enable(JsonNodeFeature.WRITE_PROPERTIES_SORTED)
        .build();

    @Test
    void exportsOpenApiSpecToFile() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode spec = objectMapper.readTree(body);

        assertThat(spec.path("openapi").asText()).startsWith("3.");
        // Auth endpoints now sit under the versioned prefix (e.g. /api/{version}/auth/login),
        // so match by suffix rather than the old unversioned literal.
        boolean hasAuthLogin = false;
        java.util.Iterator<String> pathNames = spec.path("paths").fieldNames();
        while (pathNames.hasNext()) {
            if (pathNames.next().endsWith("/auth/login")) {
                hasAuthLogin = true;
                break;
            }
        }
        assertThat(hasAuthLogin).isTrue();

        String expected = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec)
            + System.lineSeparator();

        if (Boolean.getBoolean(WRITE_PROPERTY)) {
            Files.writeString(SPEC_PATH, expected);
            return;
        }

        assertThat(Files.readString(SPEC_PATH))
            .as("%s is out of date with the controllers. Rerun with -D%s=true and commit the result.",
                SPEC_PATH, WRITE_PROPERTY)
            .isEqualTo(expected);
    }
}
