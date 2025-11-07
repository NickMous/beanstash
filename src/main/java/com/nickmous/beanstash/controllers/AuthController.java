package com.nickmous.beanstash.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/auth")
public class AuthController {
    @PostMapping("/login")
    public void login() {
        // Authentication logic would go here
    }
}
