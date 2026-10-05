package com.nickmous.beanstash.domain.search.provider;

import com.nickmous.beanstash.domain.search.SearchProvider;
import com.nickmous.beanstash.domain.search.SearchResult;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class UserSearchProvider implements SearchProvider {
    private UserRepository userRepository;

    @Override
    public String getType() {
        return "user";
    }

    @Override
    public List<SearchResult> searchForQuery(String query) {
        List<User> users = userRepository.searchForQuery(query);
        List<SearchResult> results = new ArrayList<>();

        for (User user : users) {
            results.add(new SearchResult(
                    user.getFullName() + " (@" + user.getUsername() + ")",
                    user.getUsername()
            ));
        }

        return results;
    }
}
