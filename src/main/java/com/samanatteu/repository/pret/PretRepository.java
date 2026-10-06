package com.samanatteu.repository.pret;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.pret.Pret;
import com.samanatteu.enums.pret.StatutPret;

public interface PretRepository extends JpaRepository<Pret, Long> {
    // « StatutIn » : le statut fait partie de la liste donnée
    // (SQL : statut IN ('ACTIF', 'EN_RETARD')). Une requête au lieu de deux.
    boolean existsByMembreIdAndTontineIdAndStatutIn(Long membreId, Long tontineId,
            List<StatutPret> statuts);

    List<Pret> findByTontineGestionnaireTelephone(String telephone);

    List<Pret> findByMembreTelephone(String telephone);

    // Les prêts dont le statut fait partie de la liste (ex. ACTIF et EN_RETARD)
    // dans les tontines de ce gestionnaire.
    List<Pret> findByStatutInAndTontineGestionnaireTelephone(List<StatutPret> statuts, String telephone);
}
