package com.samanatteu.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.samanatteu.enums.StatutPret;

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
@Table(name = "pret")
@Getter
@Setter
@NoArgsConstructor
public class Pret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membre_id")
    private Utilisateur membre;

    @ManyToOne
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @ManyToOne
    @JoinColumn(name = "gestionnaire_id")
    private Utilisateur gestionnaire;

    @Column(name = "montant")
    private BigDecimal montant;

    @Column(name = "taux_interet")
    private BigDecimal tauxInteret;

    @Column(name = "nb_echeances")
    private Integer nbEcheances;

    @Column(name = "date_accord")
    private LocalDate dateAccord;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutPret statut;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
