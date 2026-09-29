package com.samanatteu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Cotisation;

public interface CotisationRepository extends JpaRepository<Cotisation, Long> {
    // Requête dérivée : les cotisations dont cycle.id = ? (à la clôture).
    List<Cotisation> findByCycleId(Long cycleId);

    // cotisation → cycle → tontine → gestionnaire → telephone : les
    // cotisations des tontines de ce gestionnaire.
    List<Cotisation> findByCycleTontineGestionnaireTelephone(String telephone);

    // cotisation → participation → membre → telephone : les cotisations
    // du membre lui-même (pas celles des autres membres).
    List<Cotisation> findByParticipationMembreTelephone(String telephone);

    // WHERE cycle_id = ? AND participation_id = ? : la cotisation d'un
    // membre pour un cycle (au plus une, générée par ouvrirCycle).
    // Optional : force à traiter le cas « rien trouvé » (pas de null).
    Optional<Cotisation> findByCycleIdAndParticipationId(Long cycleId, Long participationId);


}
