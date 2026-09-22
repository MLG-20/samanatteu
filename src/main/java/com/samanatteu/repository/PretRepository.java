package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Pret;
import com.samanatteu.enums.StatutPret;

public interface PretRepository extends JpaRepository<Pret, Long> {
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
    boolean existsByMembreIdAndStatut(Long membre, StatutPret statut);
}
