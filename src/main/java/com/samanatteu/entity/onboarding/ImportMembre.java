package com.samanatteu.entity.onboarding;

import com.samanatteu.entity.commun.Auditable;
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
import lombok.Setter;

@Entity
@Table(name = "import_membres")
@Getter
@Setter
public class ImportMembre extends Auditable {
    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : Lombok 1.18.46
    // génère un constructeur vide (sans super()) pour les filles d'Auditable
    // sur ce JDK, et la JVM refuse la classe au chargement.
    public ImportMembre() {
        super();
    }
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

}
