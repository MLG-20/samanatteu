package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Pret;
import com.samanatteu.enums.StatutPret;

public interface PretRepository extends JpaRepository<Pret, Long> {
    boolean existsByMembreIdAndStatut(Long membre, StatutPret statut);
}
