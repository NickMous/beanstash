package com.nickmous.beanstash.controller.dto.user.request;

import jakarta.validation.constraints.NotBlank;

public record ReadRequest (
    @NotBlank String username
) {
}
