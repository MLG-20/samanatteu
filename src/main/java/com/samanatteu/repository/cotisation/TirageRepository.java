package com.samanatteu.repository.cotisation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.cotisation.Tirage;

public interface TirageRepository extends JpaRepository<Tirage, Long> {
    boolean existsByCycleId(Long cycleId);

    // Requête dérivée : SELECT COUNT(*) FROM tirage WHERE participation_id = ?
    // Nombre de fois qu'un membre a déjà gagné dans sa tontine.
    long countByParticipationId(Long participationId);

    // tirage → cycle → tontine → gestionnaire → telephone : les tirages
    // des tontines de ce gestionnaire.
    List<Tirage> findByCycleTontineGestionnaireTelephone(String telephone);

    // tirage → cycle → tontine → id IN (...) : tous les tirages des
    // tontines du membre, y compris ceux gagnés par les autres (US-M03).
    List<Tirage> findByCycleTontineIdIn(List<Long> tontineIds);

    // tirage → participation → membre → telephone : les tirages GAGNÉS par
    // ce membre (pas ceux des autres membres de ses tontines).
    List<Tirage> findByParticipationMembreTelephone(String telephone);

}
