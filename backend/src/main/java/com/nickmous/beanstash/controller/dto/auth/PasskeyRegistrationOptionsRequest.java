package com.nickmous.beanstash.controller.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasskeyRegistrationOptionsRequest(
    @NotBlank String username,
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) {
}
