package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.ModePaiement;
import com.samanatteu.enums.cotisation.StatutCotisation;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// participationId et cycleId (Long) au lieu de Participation/Cycle : on n'expose jamais
// une entité complète imbriquée, seulement son id (2 relations ManyToOne sur cette entité).
@Getter
@Setter
@NoArgsConstructor
public class CotisationDTO {
    private Long id;

    private Long participationId;

    private Long cycleId;

    private BigDecimal montantDu;

    private BigDecimal montantPaye;

    private BigDecimal montantCaisseDu;

    private BigDecimal montantCaissePaye;

    private LocalDateTime datePaiement;

    private ModePaiement modePaiement;

    private String reference;

    private StatutCotisation statut;

    private LocalDateTime createdAt;
}
