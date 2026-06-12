package com.nickmous.beanstash.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.nickmous.beanstash.domain.security.CustomUserDetailsService;
import com.nickmous.beanstash.entity.Authority;
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

@ExtendWith(MockitoExtension.class)
public class SecurityConfigTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void testUserDetailsService_returnsCorrectUser() {
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@email.com");
        testUser.setPassword("testpassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        when(userRepository.findByUsername("testuser")).thenReturn(testUser);

        UserDetails retrievedUser = userDetailsService.loadUserByUsername("testuser");

        assertEquals("testuser", retrievedUser.getUsername());
    }

    @Test
    void testUserDetailsService_returnsCorrectAuthorities() {
        User user = new User();
        user.setUsername("authuser");
        user.setPassword("testpassword");
        user.setFirstName("Auth");
        user.setLastName("User");

        Authority readAuth = new Authority();
        readAuth.setName("package:read");
        Authority publishAuth = new Authority();
        publishAuth.setName("package:publish");
        user.setAuthorities(Set.of(readAuth, publishAuth));

        when(userRepository.findByUsername("authuser")).thenReturn(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("authuser");

        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("package:read", "package:publish");
    }

    @Test
    void testUserDetailsService_returnsEmptyAuthoritiesWhenNoneAssigned() {
        User user = new User();
        user.setUsername("noauthuser");
        user.setPassword("testpassword");
        user.setFirstName("No");
        user.setLastName("Auth");

        when(userRepository.findByUsername("noauthuser")).thenReturn(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("noauthuser");

        assertThat(userDetails.getAuthorities()).isEmpty();
    }
}
