package com.samanatteu.repository.pret;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.enums.pret.StatutEcheancePret;

public interface EcheancePretRepository extends JpaRepository<EcheancePret, Long> {
    // StatutNot : différent de ; OrderBy...Asc : tri croissant.
    List<EcheancePret> findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(Long pretId,
            StatutEcheancePret statut);

    List<EcheancePret> findByPretTontineGestionnaireTelephone(String telephone);

    List<EcheancePret> findByPretMembreTelephone(String telephone);

    // Before : strictement avant (une échéance du jour n'est pas en retard).
    List<EcheancePret> findByStatutAndDateEcheanceBefore(StatutEcheancePret statut, LocalDate date);

    boolean existsByPretIdAndStatut(Long pretId, StatutEcheancePret statut);
}
