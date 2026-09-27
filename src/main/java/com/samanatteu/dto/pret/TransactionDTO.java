package com.samanatteu.dto.pret;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// membreId, tontineId (Long) au lieu des entités : on n'expose jamais une entité complète imbriquée.
// referenceId reste un simple Long tel quel : dans l'entité, ce n'est pas une vraie relation JPA
// (pas de @ManyToOne/@JoinColumn), juste une référence polymorphe (vers Cotisation OU Pret selon le type),
// donc rien à convertir ici.
@Getter
@Setter
@NoArgsConstructor
public class TransactionDTO {
    private Long id;

    private Long membreId;

    private Long tontineId;

    private String type;

    private BigDecimal montant;

    private String sens;

    private Long referenceId;

    private String description;

    private LocalDateTime dateTransaction;

    private LocalDateTime createdAt;
}
