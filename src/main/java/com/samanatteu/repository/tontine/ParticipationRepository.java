package com.samanatteu.repository.tontine;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.enums.tontine.StatutParticipation;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByMembreIdAndTontineId(Long membreId, Long tontineId);

    // La participation au plus grand ordre d'inscription ; Optional vide si aucun participant.
    Optional<Participation> findFirstByTontineIdOrderByOrdreInscriptionDesc(Long tontineId);

    List<Participation> findByTontineGestionnaireTelephone(String telephone);

    List<Participation> findByMembreTelephone(String telephone);

    // Les participations d'une tontine ayant ce statut (ex. ACTIF à l'ouverture
    // d'un cycle, pour savoir qui doit cotiser).
    List<Participation> findByTontineIdAndStatut(Long tontineId, StatutParticipation statut);

    // La participation d'un membre dans une tontine (vide s'il n'y est pas).
    // Sert au prêt : vérifier qu'il est bien membre de cette tontine.
    Optional<Participation> findByTontineIdAndMembreId(Long tontineId, Long membreId);

}
