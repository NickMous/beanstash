package com.nickmous.beanstash.repository;

import com.nickmous.beanstash.entity.User;
import com.nickmous.beanstash.entity.UserType;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, UUID> {
    User findByUsername(String username);

    // User-facing listings: HUMAN only, so the system sentinel (and future bots) are excluded.
    List<User> findByType(UserType type);

    @Query("""
        SELECT u FROM User u
            WHERE LOWER(u.firstName) LIKE LOWER(CONCAT("%", :query, "%"))
                OR LOWER(u.lastName) LIKE LOWER(CONCAT("%", :query, "%"))
                OR LOWER(u.username) LIKE LOWER(CONCAT("%", :query, "%"))
                OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT("%", :query, "%"))
    """)
    List<User> searchForQuery(String query);
}
