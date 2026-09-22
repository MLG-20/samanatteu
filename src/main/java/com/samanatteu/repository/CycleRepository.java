package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Cycle;

public interface CycleRepository extends JpaRepository<Cycle, Long> {
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
    boolean existsByNumeroCycleAndTontineId(Integer numeroCycle, Long tontine);
}
