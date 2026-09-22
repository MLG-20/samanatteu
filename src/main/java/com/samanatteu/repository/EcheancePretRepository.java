package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.EcheancePret;

public interface EcheancePretRepository extends JpaRepository<EcheancePret, Long> {
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
    boolean existsByNumeroEcheanceAndPretId(Integer numeroEcheance, Long pretId);
}
