package com.nickmous.beanstash.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nickmous.beanstash.domain.security.RoleService;
import com.nickmous.beanstash.entity.Role;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.RoleRepository;
import com.nickmous.beanstash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void assignDefaultRole_addsUserRoleToUser() {
        User user = new User();
        user.setUsername("newuser");

        Role userRole = new Role();
        userRole.setName("user");

        when(roleRepository.findByName("user")).thenReturn(userRole);

        roleService.assignDefaultRole(user);

        assertThat(user.getRoles()).contains(userRole);
        verify(userRepository).save(user);
    }

    @Test
    void assignDefaultRole_throwsWhenDefaultRoleMissing() {
        User user = new User();
        when(roleRepository.findByName("user")).thenReturn(null);

        assertThatThrownBy(() -> roleService.assignDefaultRole(user))
            .isInstanceOf(IllegalStateException.class);
    }
}
