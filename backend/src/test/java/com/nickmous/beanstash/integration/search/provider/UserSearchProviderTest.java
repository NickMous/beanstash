package com.nickmous.beanstash.integration.search.provider;

import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.domain.search.SearchResult;
import com.nickmous.beanstash.domain.search.provider.UserSearchProvider;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
public class UserSearchProviderTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSearchProvider userProvider;

    @BeforeEach
    void seed() {
        User alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setFirstName("Alice");
        alice.setLastName("Example");
        userRepository.save(alice);

        User bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setFirstName("Bob");
        bob.setLastName("Example");
        userRepository.save(bob);

        User sander = new User();
        sander.setUsername("sander");
        sander.setEmail("sander@example.com");
        sander.setFirstName("Sander");
        sander.setLastName("Example");
        sander.setDeletedAt(Instant.EPOCH); // This user is soft-deleted and should not appear in results.
        userRepository.save(sander);
    }

    @AfterEach
    void removeAll() {
        userRepository.deleteAll();
    }

    @Test
    public void userProvider_returnsUserSander_onQuerySearchAlic() {
        assertThat(userProvider.getType()).isEqualTo("user");

        List<SearchResult> result = userProvider.searchForQuery("Alic");

        assertThat(result).contains(new SearchResult("Alice Example (@alice)", "alice"));
        assertThat(result.size()).isEqualTo(1);
    }
}
