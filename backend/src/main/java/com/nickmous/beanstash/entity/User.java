package com.nickmous.beanstash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Data
@Entity
@SQLDelete(sql = "UPDATE \"user\" SET deleted_at = now() WHERE id = ?")
// Soft-deleted users are hidden from every JPA query. The system sentinel stays
// loadable here (the AuditLog.actor association resolves to it); it is kept out of
// user-facing endpoints by querying for UserType.HUMAN instead.
@SQLRestriction("deleted_at IS NULL")
@Table(name = "\"user\"")
public class User {

    @Id
    @GeneratedValue
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private byte[] totpSecret;
    private boolean totpEnabled;
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType type = UserType.HUMAN;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_authority",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private Set<Authority> authorities = new HashSet<>();
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
