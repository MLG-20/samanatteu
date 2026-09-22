package com.samanatteu.dto.tontine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.samanatteu.enums.StatutCycle;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO : jamais d'annotations JPA, cette classe n'est ni stockée ni lue en base.
// tontineId (Long) au lieu de Tontine : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
@NoArgsConstructor
public class CycleDTO {
    private Long id;

    private Long tontineId;

    private Integer numeroCycle;

    private LocalDate dateDebut;

    private LocalDate dateFinPrevue;

    private LocalDate dateFinReelle;

    private BigDecimal montantAttendu;

    private BigDecimal montantCollecte;

    private StatutCycle statut;

    private LocalDateTime createdAt;

}
