package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Participation;

public interface ParticipationRepository extends JpaRepository<Participation, Long>{
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
}