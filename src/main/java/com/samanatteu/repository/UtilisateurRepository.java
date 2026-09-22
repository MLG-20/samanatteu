package com.samanatteu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Utilisateur;







public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    // Pas de corps de méthode : Spring Data JPA lit le NOM "existsByEmail" et génère
    // lui-même le SQL équivalent à "SELECT COUNT(*) > 0 FROM utilisateur WHERE email = ?".
    // "existsBy" = mot-clé reconnu par Spring, "Email" = doit correspondre EXACTEMENT
    // au nom de l'attribut dans Utilisateur.java (avec la 1ère lettre en majuscule).
    boolean existsByEmail(String email);
    boolean existsByTelephone(String telephone);

    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByTelephone(String telephone);
}
