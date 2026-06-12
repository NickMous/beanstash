package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.entity.Authority;
import com.nickmous.beanstash.repository.AuthorityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
public class AuthorityRepositoryTests {

    @Autowired
    private AuthorityRepository authorityRepository;

    @Test
    void findByName_returnsSeededAuthority() {
        Authority authority = authorityRepository.findByName("package:read");
        assertThat(authority).isNotNull();
        assertThat(authority.getName()).isEqualTo("package:read");
    }

    @Test
    void findByName_returnsNullForNonexistent() {
        Authority authority = authorityRepository.findByName("nonexistent:permission");
        assertThat(authority).isNull();
    }
}
