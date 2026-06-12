package com.nickmous.beanstash.controller.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PathVariable;

public record ReadRequest (
    @NotBlank @PathVariable String username
) {
}
