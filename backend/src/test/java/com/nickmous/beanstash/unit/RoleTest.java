package com.nickmous.beanstash.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nickmous.beanstash.entity.Authority;
import com.nickmous.beanstash.entity.Role;
import com.nickmous.beanstash.entity.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoleTest {

    @Test
    void role_canBeCreatedWithNameAndId() {
        Role role = new Role();
        UUID id = UUID.randomUUID();
        role.setId(id);
        role.setName("user");

        assertThat(role.getId()).isEqualTo(id);
        assertThat(role.getName()).isEqualTo("user");
    }

    @Test
    void role_authoritiesDefaultToEmptySet() {
        Role role = new Role();
        assertThat(role.getAuthorities()).isNotNull().isEmpty();
    }

    @Test
    void role_canHaveAuthoritiesAssigned() {
        Role role = new Role();
        Authority authority = new Authority();
        authority.setName("package:read");

        role.setAuthorities(Set.of(authority));

        assertThat(role.getAuthorities()).hasSize(1);
        assertThat(role.getAuthorities().iterator().next().getName()).isEqualTo("package:read");
    }

    @Test
    void user_rolesDefaultToEmptySet() {
        User user = new User();
        assertThat(user.getRoles()).isNotNull().isEmpty();
    }

    @Test
    void user_canHaveRolesAssigned() {
        User user = new User();
        Role role = new Role();
        role.setName("user");

        user.setRoles(Set.of(role));

        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles().iterator().next().getName()).isEqualTo("user");
    }
}
