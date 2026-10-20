package com.samanatteu.dto.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.enums.onboarding.StatutImportMembre;

import lombok.Getter;
import lombok.Setter;

// tontineId (Long) au lieu de Tontine : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
public class ImportMembreDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public ImportMembreDTO() {
        super();
    }

    private Long id;

    private Long tontineId;

    private String fichierNom;

    private Integer nbMembresTotal;

    private Integer nbImportes;

    private Integer nbErreurs;

    private String erreursDetail;

    private StatutImportMembre statut;

    private LocalDateTime createdAt;
}
