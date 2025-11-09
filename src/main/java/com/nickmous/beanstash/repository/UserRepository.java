package com.nickmous.beanstash.repository;

import com.nickmous.beanstash.entity.User;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends BaseRepository<User, Long> {

    // Find active user by username (not soft deleted)
    @Query("SELECT u FROM User u WHERE u.username = ?1 AND u.deletedAt IS NULL")
    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    // Find active user by email (not soft deleted)
    @Query("SELECT u FROM User u WHERE u.email = ?1 AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
}
