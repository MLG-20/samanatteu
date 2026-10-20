package com.samanatteu.repository.abonnement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.abonnement.Formule;

// Les formules d'abonnement : des DONNÉES, modifiables par l'ADMIN.
public interface FormuleRepository extends JpaRepository<Formule, Long> {

    // Les formules qu'on peut proposer à une nouvelle cliente (les autres
    // restent en base pour les abonnements existants).
    List<Formule> findByActiveTrueOrderByPrixMensuelAsc();

    Optional<Formule> findByNom(String nom);
}
