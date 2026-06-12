package com.nickmous.beanstash.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nickmous.beanstash.entity.Authority;
import com.nickmous.beanstash.entity.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorityTest {

    @Test
    void authority_canBeCreatedWithNameAndId() {
        Authority authority = new Authority();
        UUID id = UUID.randomUUID();
        authority.setId(id);
        authority.setName("package:read");

        assertThat(authority.getId()).isEqualTo(id);
        assertThat(authority.getName()).isEqualTo("package:read");
    }

    @Test
    void user_canHaveAuthoritiesAssigned() {
        User user = new User();
        Authority authority = new Authority();
        authority.setName("package:read");

        user.setAuthorities(Set.of(authority));

        assertThat(user.getAuthorities()).hasSize(1);
        assertThat(user.getAuthorities().iterator().next().getName()).isEqualTo("package:read");
    }

    @Test
    void user_authoritiesDefaultToEmptySet() {
        User user = new User();
        assertThat(user.getAuthorities()).isNotNull().isEmpty();
    }
}
