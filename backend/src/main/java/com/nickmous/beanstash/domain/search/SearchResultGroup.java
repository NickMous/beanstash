package com.nickmous.beanstash.domain.search;

import java.util.List;

public record SearchResultGroup(
        String type,
        List<SearchResult> results
) {
}
