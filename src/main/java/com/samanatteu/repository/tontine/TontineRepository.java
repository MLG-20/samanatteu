package com.samanatteu.repository.tontine;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.tontine.StatutTontine;

public interface TontineRepository extends JpaRepository<Tontine, Long> {
    List<Tontine> findByGestionnaireTelephone(String telephone);

    // Le nombre de tontines d'une gestionnaire : pour le quota de la formule
    // d'abonnement. On compte en base (COUNT) au lieu de charger les lignes
    // pour les compter.
    long countByGestionnaireTelephone(String telephone);

    Long countByStatut(StatutTontine statut);
}
