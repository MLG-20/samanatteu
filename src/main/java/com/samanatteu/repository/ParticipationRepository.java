package com.samanatteu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Participation;

public interface ParticipationRepository extends JpaRepository<Participation, Long>{
    boolean existsByMembreIdAndTontineId(Long membreId, Long tontineId);

    // La participation au plus grand ordre d'inscription ; Optional vide si aucun participant.
    Optional<Participation> findFirstByTontineIdOrderByOrdreInscriptionDesc(Long tontineId);

    List<Participation> findByTontineGestionnaireTelephone(String telephone);
    List<Participation> findByMembreTelephone(String telephone);


}
