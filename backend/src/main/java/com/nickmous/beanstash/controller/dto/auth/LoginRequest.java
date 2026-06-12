package com.nickmous.beanstash.controller.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    String totpCode
) {
}
