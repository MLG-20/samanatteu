package com.samanatteu.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

import com.samanatteu.dto.abonnement.AbonnementDTO;

import lombok.Getter;
import lombok.Setter;

// La fiche complète d'une gestionnaire, pour l'ADMIN : qui elle est, ce qu'elle
// paie, ce qu'elle utilise. Tout ce qu'il faut pour gérer une cliente, rien de ce
// qui appartient à ses membres (ni nom, ni téléphone, ni montant).
@Getter
@Setter
public class FicheGestionnaireDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public FicheGestionnaireDTO() {
        super();
    }

    // ---- Identité
    private Long id;

    private String nom;

    private String prenom;

    private String telephone;

    private String email;

    private Boolean actif;

    private LocalDateTime createdAt;

    // ---- Abonnement : null si elle n'en a jamais eu.
    private AbonnementDTO abonnement;

    // ---- Utilisation
    // Personnes distinctes dans toutes ses tontines (hors SORTI).
    private Long nbMembres;

    // Tontines non TERMINEE : c'est ce que le quota de la formule limite.
    private Long nbTontinesEnCours;

    private List<TontineFicheDTO> tontines;
}
