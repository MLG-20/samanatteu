package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Tirage;

public interface TirageRepository extends JpaRepository<Tirage, Long> {
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
    boolean existsByCycleId(Long cycleId);
}
