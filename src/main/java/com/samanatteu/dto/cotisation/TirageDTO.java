package com.samanatteu.dto.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.cotisation.StatutTirage;

import lombok.Getter;
import lombok.Setter;

// cycleId et participationId (Long) au lieu de Cycle/Participation : on n'expose jamais
// une entité complète imbriquée, seulement son id.
@Getter
@Setter
public class TirageDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public TirageDTO() {
        super();
    }

    private Long id;

    private Long cycleId;

    private Long participationId;

    private BigDecimal montantGagne;

    private BigDecimal montantVerse;

    private LocalDateTime dateTirage;

    private LocalDateTime dateVersement;

    private StatutTirage statut;

    private LocalDateTime createdAt;
}
