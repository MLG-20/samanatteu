package com.samanatteu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Cycle;
import com.samanatteu.enums.StatutCycle;

public interface CycleRepository extends JpaRepository<Cycle, Long> {
    // Requêtes dérivées : Spring écrit le SQL à partir du nom de la méthode.
    // « Existe-t-il un cycle de cette tontine avec ce statut ? »
    boolean existsByTontineIdAndStatut(Long tontineId, StatutCycle statut);

    // Le cycle au plus grand numéro (tri décroissant, on garde le premier) ;
    // Optional vide si la tontine n'a encore aucun cycle.
    Optional<Cycle> findFirstByTontineIdOrderByNumeroCycleDesc(Long tontineId);

    // Chemin cycle → tontine → gestionnaire → telephone : les cycles des
    // tontines gérées par ce gestionnaire.
    List<Cycle> findByTontineGestionnaireTelephone(String telephone);

    // « In » : tontine.id fait partie de la liste → SQL « WHERE tontine_id
    // IN (4, 7, 12) ». Une seule requête pour toutes les tontines d'un membre.
    List<Cycle> findByTontineIdIn(List<Long> tontineIds);

}
