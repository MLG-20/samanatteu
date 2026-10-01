package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.cotisation.StatutTirage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// cycleId et participationId (Long) au lieu de Cycle/Participation : on n'expose jamais
// une entité complète imbriquée, seulement son id.
@Getter
@Setter
@NoArgsConstructor
public class TirageDTO {
    private Long id;

    private Long cycleId;

    private Long participationId;

    private BigDecimal montantGagne;

    private BigDecimal montantVerse;

    private LocalDateTime dateTirage;

    private LocalDateTime dateVersement;

    private StatutTirage statut;

    private LocalDateTime createdAt;
}
