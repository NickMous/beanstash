package com.nickmous.beanstash.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    @PreAuthorize("hasAuthority('package:read')")
    public String home() {
        return "Welcome to BeanStash API!";
    }
}
