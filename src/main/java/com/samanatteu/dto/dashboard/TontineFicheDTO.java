package com.samanatteu.dto.dashboard;

import java.time.LocalDateTime;

import com.samanatteu.enums.tontine.FrequenceTontine;
import com.samanatteu.enums.tontine.StatutTontine;

import lombok.Getter;
import lombok.Setter;

// Une tontine vue par l'ADMIN, sur la fiche d'un gestionnaire. Volontairement
// SANS argent (ni part, ni caisse, ni cotisation) et sans aucun membre nommé :
// frontière SaaS, l'ADMIN voit qu'une tontine existe, pas ce qu'elle contient.
@Getter
@Setter
public class TontineFicheDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public TontineFicheDTO() {
        super();
    }

    private Long id;

    private String nom;

    private StatutTontine statut;

    private FrequenceTontine frequence;

    // Tous les 'intervalle' jours / semaines / mois.
    private Integer intervalle;

    // Membres hors SORTI (ACTIF + SUSPENDU) : un nombre, jamais la liste.
    private Long nbMembres;

    private LocalDateTime createdAt;
}
