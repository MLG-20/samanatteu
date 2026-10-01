package com.samanatteu.entity.onboarding;

import java.time.LocalDateTime;

import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.onboarding.StatutImportMembre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "import_membres")
@Getter
@Setter
@NoArgsConstructor
public class ImportMembre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @Column(name = "fichier_nom")
    private String fichierNom;

    @Column(name = "nb_membres_total")
    private Integer nbMembresTotal;

    @Column(name = "nb_importes")
    private Integer nbImportes;

    @Column(name = "nb_erreurs")
    private Integer nbErreurs;

    @Column(name = "erreurs_detail")
    private String erreursDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutImportMembre statut;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
