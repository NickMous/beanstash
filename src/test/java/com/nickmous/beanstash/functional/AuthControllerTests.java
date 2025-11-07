package com.nickmous.beanstash.functional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
public class AuthControllerTests {

    @Test
    public void loginPost_ShouldReturn200() {
        when()
            .post("/auth/login")
        .then()
            .statusCode(200);
    }
}
