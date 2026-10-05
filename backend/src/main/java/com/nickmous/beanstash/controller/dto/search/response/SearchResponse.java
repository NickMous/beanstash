package com.nickmous.beanstash.controller.dto.search.response;

import com.nickmous.beanstash.domain.search.SearchResultGroup;

import java.util.List;

public record SearchResponse(
        List<SearchResultGroup> results
) {
}
