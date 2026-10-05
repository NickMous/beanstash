package com.nickmous.beanstash.domain.search;

import java.util.List;

public interface SearchProvider {
    String getType();
    List<SearchResult> searchForQuery(String query);
}
