package com.samanatteu.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.auth.VerificationEmail;

public interface VerificationEmailRepository extends JpaRepository<VerificationEmail, Long> {
    // Le lien cliqué, retrouvé par l'empreinte du jeton reçu (colonne UNIQUE, indexée).
    Optional<VerificationEmail> findByJetonHache(String jetonHache);

    // Le dernier lien envoyé à ce compte : on y lit la formule choisie quand il faut renvoyer l'email.
    Optional<VerificationEmail> findFirstByUtilisateurIdOrderByCreatedAtDesc(Long utilisateurId);
}
