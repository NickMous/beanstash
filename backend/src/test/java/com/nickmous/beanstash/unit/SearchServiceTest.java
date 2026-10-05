package com.nickmous.beanstash.unit;

import com.nickmous.beanstash.domain.search.SearchProvider;
import com.nickmous.beanstash.domain.search.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {
    @Mock
    private SearchProvider searchProvider;

    private SearchService service;

    @BeforeEach
    void beforeAll() {
        service = new SearchService(List.of(searchProvider));
    }

    @Test
    public void searchService_retrievesAllSearchProviders() {
        when(searchProvider.getType()).thenReturn("mocked");
        when(searchProvider.searchForQuery("test")).thenReturn(new ArrayList<>());

        assertThat(service.searchForQuery("test")).isEqualTo(new ArrayList<>());

        verify(searchProvider, times(1)).getType();
        verify(searchProvider, times(1)).searchForQuery("test");
    }
}
