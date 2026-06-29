package com.nickmous.beanstash.controller.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyTotpRequest(
    @NotBlank String username,
    @NotBlank @Size(min = 6, max = 6) String code
) {
}
