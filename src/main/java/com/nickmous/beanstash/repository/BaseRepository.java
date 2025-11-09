package com.nickmous.beanstash.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T, K> extends JpaRepository<T, K> {
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    List<T> findAllNotDeleted();

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = ?1 AND e.deletedAt IS NULL")
    Optional<T> findByIdNotDeleted(K id);

    @Override
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = ?1 AND e.deletedAt IS NULL")
    void deleteById(K id);

    @Modifying
    @Query("DELETE FROM #{#entityName} e WHERE e.id = ?1")
    void hardDeleteById(K id);
}
