package com.nickmous.beanstash.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nickmous.beanstash.domain.security.CustomUserDetailsService;
import com.nickmous.beanstash.entity.Authority;
import com.nickmous.beanstash.entity.Role;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private static Authority authority(String name) {
        Authority authority = new Authority();
        authority.setName(name);
        return authority;
    }

    @Test
    void loadUserByUsername_includesDirectAuthorities() {
        User user = new User();
        user.setUsername("alice");
        user.setAuthorities(Set.of(authority("package:read")));

        when(userRepository.findByUsername("alice")).thenReturn(user);

        UserDetails details = customUserDetailsService.loadUserByUsername("alice");

        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("package:read");
    }

    @Test
    void loadUserByUsername_includesAuthoritiesFromRoles() {
        Role role = new Role();
        role.setName("user");
        role.setAuthorities(Set.of(authority("package:read"), authority("package:write")));

        User user = new User();
        user.setUsername("bob");
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("bob")).thenReturn(user);

        UserDetails details = customUserDetailsService.loadUserByUsername("bob");

        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("package:read", "package:write");
    }

    @Test
    void loadUserByUsername_mergesAndDeduplicatesDirectAndRoleAuthorities() {
        Role role = new Role();
        role.setName("user");
        role.setAuthorities(Set.of(authority("package:read"), authority("package:write")));

        User user = new User();
        user.setUsername("carol");
        user.setAuthorities(Set.of(authority("package:read"), authority("package:delete")));
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("carol")).thenReturn(user);

        UserDetails details = customUserDetailsService.loadUserByUsername("carol");

        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("package:read", "package:write", "package:delete");
    }

    @Test
    void loadUserByUsername_throwsWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class);
    }
}
