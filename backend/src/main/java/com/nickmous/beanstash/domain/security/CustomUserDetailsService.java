package com.nickmous.beanstash.domain.security;

import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private UserRepository userRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        Set<String> authorityNames = new LinkedHashSet<>();
        user.getAuthorities()
            .forEach(authority -> authorityNames.add(authority.getName()));
        user.getRoles()
            .forEach(role -> role.getAuthorities()
                .forEach(authority -> authorityNames.add(authority.getName())));

        List<SimpleGrantedAuthority> grantedAuthorities = authorityNames.stream()
            .map(SimpleGrantedAuthority::new)
            .toList();

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword() != null ? user.getPassword() : "")
            .authorities(grantedAuthorities)
            .build();
    }
}
