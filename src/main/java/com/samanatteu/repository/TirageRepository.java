package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Tirage;

public interface TirageRepository extends JpaRepository<Tirage, Long> {
    boolean existsByCycleId(Long cycleId);
}
