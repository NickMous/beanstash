package com.nickmous.beanstash.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nickmous.beanstash.configuration.TestcontainersConfig;
import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ContextConfiguration(initializers = TestcontainersConfig.Initializer.class)
@ActiveProfiles("test")
public class UserSoftDeleteTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void delete_softDeletesUser_andHidesItFromFinders() {
        User user = new User();
        user.setUsername("softdelete-target");
        user.setEmail("softdelete@example.com");
        UUID id = userRepository.save(user).getId();

        userRepository.delete(user);

        // Hidden from the standard finders (@SQLRestriction adds "deleted_at IS NULL").
        assertThat(userRepository.findByUsername("softdelete-target")).isNull();
        assertThat(userRepository.findById(id)).isEmpty();
        assertThat(userRepository.findAll())
            .extracting(User::getId)
            .doesNotContain(id);

        // The row is still physically present with deleted_at stamped (@SQLDelete, not a hard delete).
        Integer softDeletedRows = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM \"user\" WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, id);
        assertThat(softDeletedRows).isEqualTo(1);
    }
}
