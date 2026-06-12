package com.nickmous.beanstash.repository;

import com.nickmous.beanstash.entity.Role;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface RoleRepository extends CrudRepository<Role, UUID> {
    Role findByName(String name);
}
