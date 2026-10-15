package com.samanatteu.repository.tontine;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.tontine.StatutTontine;

public interface TontineRepository extends JpaRepository<Tontine, Long> {
    List<Tontine> findByGestionnaireTelephone(String telephone);

    Long countByStatut(StatutTontine statut);
}
