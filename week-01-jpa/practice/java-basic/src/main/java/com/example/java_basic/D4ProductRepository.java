package com.example.java_basic;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface D4ProductRepository extends JpaRepository<D4_Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from D4_Product p where p.id = :id")
    Optional<D4_Product> findByIdWithLock(Long id);
}
