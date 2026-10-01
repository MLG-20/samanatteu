package com.samanatteu.dto.pret;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.samanatteu.enums.pret.StatutPret;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// membreId, tontineId, gestionnaireId (Long) au lieu des entités Utilisateur/Tontine :
// 3 relations ManyToOne sur cette entité, chacune ramenée à son seul id. gestionnaireId
// est notamment crucial : si on avait mis Utilisateur gestionnaire, motDePasse aurait fuité ici aussi.
@Getter
@Setter
@NoArgsConstructor
public class PretDTO {
    private Long id;

    private Long membreId;

    private Long tontineId;

    private Long gestionnaireId;

    private BigDecimal montant;

    private BigDecimal tauxInteret;

    private BigDecimal montantInteret;

    private Integer nbEcheances;

    private LocalDate dateAccord;

    private LocalDate dateDebutRemboursement;

    private StatutPret statut;

    private LocalDateTime createdAt;
}
