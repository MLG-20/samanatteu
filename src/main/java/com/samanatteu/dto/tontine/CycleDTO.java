package com.samanatteu.dto.tontine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.samanatteu.enums.tontine.StatutCycle;

import lombok.Getter;
import lombok.Setter;

// tontineId (Long) au lieu de Tontine : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
public class CycleDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public CycleDTO() {
        super();
    }

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
