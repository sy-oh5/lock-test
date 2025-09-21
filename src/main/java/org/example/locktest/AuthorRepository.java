package org.example.locktest;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Author a where a.id = :id")
    Optional<Author> findByIdForUpdateAsPessimistic(@Param("id") Long id);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select a from Author a where a.id = :id")
    Optional<Author> findByIdForUpdateAsOptimistic(@Param("id") Long id);
}
