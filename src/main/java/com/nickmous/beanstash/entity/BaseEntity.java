package com.nickmous.beanstash.entity;

import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Data;

@MappedSuperclass
@Data
public class BaseEntity {

    protected BaseEntity() {
        // Protected constructor to prevent direct instantiation
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
