package com.samanatteu.repository.onboarding;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.onboarding.ImportMembre;

public interface ImportMembreRepository extends JpaRepository <ImportMembre, Long>{
    // Requête dérivée : traverse import → tontine → gestionnaire → telephone,
    // et trie du plus récent au plus ancien.
    List<ImportMembre> findByTontineGestionnaireTelephoneOrderByCreatedAtDesc(String telephone);
}
