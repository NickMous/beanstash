package com.nickmous.beanstash.domain.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Data
@AllArgsConstructor
public class SearchService {
    private List<SearchProvider> searchProviders;

    public List<SearchResultGroup> searchForQuery(String query) {
        List<SearchResultGroup> results = new ArrayList<>();

        for (SearchProvider searchProvider : searchProviders) {
            SearchResultGroup resultGroup = new SearchResultGroup(
                    searchProvider.getType(),
                    searchProvider.searchForQuery(query)
            );

            if (resultGroup.results().isEmpty()) {
                continue;
            }

            results.add(resultGroup);
        }

        return results;
    }
}
