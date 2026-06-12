package com.nickmous.beanstash.domain.security;

import com.nickmous.beanstash.entity.Role;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.RoleRepository;
import com.nickmous.beanstash.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RoleService {

    private static final String DEFAULT_ROLE = "user";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public void assignDefaultRole(User user) {
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE);
        if (defaultRole == null) {
            throw new IllegalStateException("Default role '" + DEFAULT_ROLE + "' not found in database");
        }
        user.getRoles().add(defaultRole);
        userRepository.save(user);
    }
}
