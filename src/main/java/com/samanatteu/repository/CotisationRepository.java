package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Cotisation;

public interface CotisationRepository extends JpaRepository<Cotisation, Long> {
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
}
