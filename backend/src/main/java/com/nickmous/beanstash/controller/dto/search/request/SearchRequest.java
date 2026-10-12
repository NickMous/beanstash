package com.nickmous.beanstash.controller.dto.search.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotNull @Size(min = 4, max = 100) String query
) {
}
