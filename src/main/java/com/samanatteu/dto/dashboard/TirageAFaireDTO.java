package com.samanatteu.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Une ligne « Tirages à faire » du tableau de bord gestionnaire : un cycle
// clôturé dont le gagnant n'a pas encore été tiré au sort.
@Getter
@Setter
@NoArgsConstructor
public class TirageAFaireDTO {
    // Id du CYCLE (pas d'un tirage : il n'existe pas encore) ; le front
    // s'en sert pour lancer POST /cycle/{id}/tirage.
    private Long cycleId;

    private String tontineNom;

    private Integer numeroCycle;

    // dateFinReelle du cycle : depuis quand le tirage attend.
    private LocalDate dateCloture;

    // Ce qu'il y a réellement dans la cagnotte...
    private BigDecimal montantCollecte;

    // ...et ce que le gagnant doit recevoir au total (l'écart = les retards).
    private BigDecimal montantAttendu;
}
