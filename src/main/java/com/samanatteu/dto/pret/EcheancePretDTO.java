package com.samanatteu.dto.pret;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.samanatteu.enums.StatutEcheancePret;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO : jamais d'annotations JPA, cette classe n'est ni stockée ni lue en base.
// pretId (Long) au lieu de Pret : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
@NoArgsConstructor
public class EcheancePretDTO {
    private Long id;

    private Long pretId;

    private Integer numeroEcheance;

    private BigDecimal montantDu;

    private BigDecimal montantPaye;

    private LocalDate dateEcheance;

    private LocalDateTime datePaiement;

    private StatutEcheancePret statut;
}
