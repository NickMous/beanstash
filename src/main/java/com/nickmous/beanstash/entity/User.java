package com.nickmous.beanstash.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "users")
public class User extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String username;

    private String email;

    private String password;

    private boolean isActive;
}
