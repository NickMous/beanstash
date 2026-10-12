package com.nickmous.beanstash.controller;

import com.nickmous.beanstash.controller.dto.search.request.SearchRequest;
import com.nickmous.beanstash.controller.dto.search.response.SearchResponse;
import com.nickmous.beanstash.domain.search.SearchService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class SearchController {
    private SearchService searchService;

    @GetMapping(path = "/search", version = "v1")
    SearchResponse search(@ParameterObject @Valid SearchRequest request) {
        return new SearchResponse(searchService.searchForQuery(request.query()));
    }
}
