package com.samanatteu.dto.pret;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.ModePaiement;
import com.samanatteu.enums.pret.SensTransaction;
import com.samanatteu.enums.pret.TypeTransaction;

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

    private TypeTransaction type;

    private BigDecimal montant;

    private SensTransaction sens;

    private ModePaiement modePaiement;

    // Référence externe (Wave, Orange Money...), à ne pas confondre avec
    // referenceId (id de la cotisation ou du prêt dans notre base).
    private String reference;

    private Long referenceId;

    private String description;

    private LocalDateTime dateTransaction;

    private LocalDateTime createdAt;
}
