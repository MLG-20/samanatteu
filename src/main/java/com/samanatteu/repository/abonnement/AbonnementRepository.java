package com.samanatteu.repository.abonnement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.abonnement.Abonnement;

// Les abonnements des gestionnaires.
//
// La règle d'accès : l'ADMIN voit TOUS les abonnements (ce sont les données
// commerciales de la plateforme), la gestionnaire voit le SIEN. C'est filtré
// dans le service, pas ici : la requête est la même, seule la condition change.
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    // L'abonnement d'une cliente (s'il y en a un).
    Optional<Abonnement> findByUtilisateurId(Long utilisateurId);

    // Le même, mais par le TÉLÉPHONE de la cliente : c'est par là qu'on
    // l'identifie partout ailleurs dans le projet (le téléphone est l'identité
    // du compte). Requête dérivée à travers la relation.
    Optional<Abonnement> findByUtilisateurTelephone(String telephone);

    // Tous les abonnements, pour l'ADMIN.
    List<Abonnement> findAllByOrderByDateFinAsc();
}
