package com.samanatteu.repository.tontine;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.tontine.StatutTontine;

public interface TontineRepository extends JpaRepository<Tontine, Long> {
    List<Tontine> findByGestionnaireTelephone(String telephone);

    // Les tontines d'un gestionnaire, les plus récentes d'abord (fiche ADMIN).
    List<Tontine> findByGestionnaireIdOrderByCreatedAtDesc(Long gestionnaireId);

    // Le nombre de tontines EN COURS d'une gestionnaire : pour le quota de la
    // formule d'abonnement. On l'appelle avec TERMINEE : une tontine finie ne
    // compte plus (le quota limite le travail du moment, pas l'historique).
    // "StatutNot" = WHERE statut <> ?. On compte en base (COUNT) au lieu de
    // charger les lignes pour les compter.
    long countByGestionnaireTelephoneAndStatutNot(String telephone, StatutTontine statut);

    Long countByStatut(StatutTontine statut);
}
