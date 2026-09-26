package com.samanatteu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Participation;

public interface ParticipationRepository extends JpaRepository<Participation, Long>{
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
    // "existsBy" -> renvoie true/false. Cherche s'il existe déjà une participation pour CE membre
    // ET CETTE tontine (sert à refuser un doublon). L'ordre des paramètres suit l'ordre du nom.
    boolean existsByMembreIdAndTontineId(Long membreId, Long tontineId);

    // "findFirst ... OrderBy ... Desc" -> parmi les participations de cette tontine, triées par
    // ordre d'inscription décroissant, ne garde que la première : celle qui a le PLUS GRAND ordre.
    // Optional vide si la tontine n'a encore aucun participant.
    Optional<Participation> findFirstByTontineIdOrderByOrdreInscriptionDesc(Long tontineId);

    List<Participation> findByTontineGestionnaireTelephone(String telephone);
    List<Participation> findByMembreTelephone(String telephone);


}
