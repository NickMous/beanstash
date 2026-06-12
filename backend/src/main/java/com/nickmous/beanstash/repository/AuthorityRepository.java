package com.nickmous.beanstash.repository;

import com.nickmous.beanstash.entity.Authority;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface AuthorityRepository extends CrudRepository<Authority, UUID> {
    Authority findByName(String name);
}
