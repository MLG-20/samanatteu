package com.samanatteu.dto.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.enums.onboarding.StatutImportMembre;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// tontineId (Long) au lieu de Tontine : on n'expose jamais une entité complète imbriquée, juste son id.
@Getter
@Setter
@NoArgsConstructor
public class ImportMembreDTO {
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
