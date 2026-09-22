package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.StatutTirage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO : jamais d'annotations JPA, cette classe n'est ni stockée ni lue en base.
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

    private LocalDateTime dateTirage;

    private LocalDateTime dateVersement;

    private StatutTirage statut;

    private LocalDateTime createdAt;
}
