package com.samanatteu.dto.abonnement;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// La demande d'activation d'un abonnement, faite par l'ADMIN.
//
// L'activation est MANUELLE : pas de paiement réel pour l'instant (Wave, Orange
// Money…). C'est ce qui permet de développer et de tester sans passer par un
// prestataire de paiement. Le jour où le paiement arrive, cette route disparaît
// au profit d'un webhook du prestataire.
@Getter
@Setter
public class ActivationAbonnementDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public ActivationAbonnementDTO() {
        super();
    }


    // La cliente concernée (son id).
    @NotNull(message = "L'utilisateur est obligatoire.")
    private Long utilisateurId;

    // La formule choisie (son identifiant : les formules sont des DONNÉES,
    // modifiables par l'ADMIN — pas des noms figés dans le code).
    @NotNull(message = "La formule est obligatoire.")
    private Long formuleId;

    // La durée, en mois. 1 = un mois d'abonnement.
    @NotNull(message = "La durée est obligatoire.")
    private Integer dureeMois;
}
