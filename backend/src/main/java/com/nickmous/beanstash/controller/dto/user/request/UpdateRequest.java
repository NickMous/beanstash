package com.nickmous.beanstash.controller.dto.user.request;

import jakarta.validation.constraints.Email;

public record UpdateRequest(
    @Email String email,
    String firstName,
    String lastName
) {
}
