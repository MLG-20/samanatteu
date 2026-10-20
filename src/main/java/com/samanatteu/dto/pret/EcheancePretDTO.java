package com.samanatteu.dto.pret;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.samanatteu.enums.pret.StatutEcheancePret;

import lombok.Getter;
import lombok.Setter;

// pretId (Long) au lieu de Pret : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
public class EcheancePretDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public EcheancePretDTO() {
        super();
    }

    private Long id;

    private Long pretId;

    private Integer numeroEcheance;

    private BigDecimal montantDu;

    private BigDecimal montantPaye;

    private LocalDate dateEcheance;

    private LocalDateTime datePaiement;

    private StatutEcheancePret statut;
}
