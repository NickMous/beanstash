package com.nickmous.beanstash.end2end;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointE2ETest {

    @LocalServerPort
    private int port;

    @Test
    void healthEndpointReturnsUp() {
        given()
            .port(port)
        .when()
            .get("/actuator/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}
