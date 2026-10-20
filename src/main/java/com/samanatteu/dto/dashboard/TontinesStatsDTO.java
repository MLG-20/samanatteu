package com.samanatteu.dto.dashboard;

import lombok.Getter;
import lombok.Setter;

// Bloc « Tontines » du tableau de bord admin : combien de tontines par
// statut, sans nom ni montant (frontière SaaS : l'ADMIN ne voit pas le contenu
// des tontines). Les chiffres sont calculés en base (COUNT), d'où un DTO
// dédié et non l'entité Tontine.
@Getter
@Setter
public class TontinesStatsDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public TontinesStatsDTO() {
        super();
    }


    // Toutes les tontines, tous statuts confondus (count() du repository).
    private Long total;

    // Détail par statut : la somme des quatre vaut total.
    private Long nbEnAttente;

    private Long nbActive;

    private Long nbSuspendue;

    private Long nbTerminee;
}
