package com.samanatteu.dto.pret;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.samanatteu.enums.pret.StatutPret;

import lombok.Getter;
import lombok.Setter;

// membreId, tontineId, gestionnaireId (Long) au lieu des entités Utilisateur/Tontine :
// 3 relations ManyToOne sur cette entité, chacune ramenée à son seul id. gestionnaireId
// est notamment crucial : si on avait mis Utilisateur gestionnaire, motDePasse aurait fuité ici aussi.
@Getter
@Setter
public class PretDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public PretDTO() {
        super();
    }

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
