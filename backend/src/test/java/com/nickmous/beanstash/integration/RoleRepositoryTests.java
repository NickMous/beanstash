package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.entity.Role;
import com.nickmous.beanstash.repository.RoleRepository;
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
public class RoleRepositoryTests {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findByName_returnsSeededRole() {
        Role role = roleRepository.findByName("user");
        assertThat(role).isNotNull();
        assertThat(role.getName()).isEqualTo("user");
    }

    @Test
    void findByName_seededRoleGrantsPackageRead() {
        Role role = roleRepository.findByName("user");
        assertThat(role).isNotNull();
        assertThat(role.getAuthorities())
            .extracting(authority -> authority.getName())
            .contains("package:read");
    }

    @Test
    void findByName_returnsNullForNonexistent() {
        Role role = roleRepository.findByName("nonexistent");
        assertThat(role).isNull();
    }
}
