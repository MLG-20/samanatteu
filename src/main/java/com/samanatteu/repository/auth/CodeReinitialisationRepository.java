package com.samanatteu.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.auth.CodeReinitialisation;

public interface CodeReinitialisationRepository extends JpaRepository<CodeReinitialisation, Long> {
    // Le dernier code demandé : après deux demandes, seul le SMS le plus récent compte.
    Optional<CodeReinitialisation> findFirstByUtilisateurIdOrderByCreatedAtDesc(Long utilisateurId);
}
