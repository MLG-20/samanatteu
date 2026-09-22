package com.samanatteu.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.enums.StatutTontine;

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
@Table(name = "tontine")
@Getter
@Setter
@NoArgsConstructor

public class Tontine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom")
    private String nom;

    @Column(name = "montant_part")
    private BigDecimal montantPart;

    @Column(name = "frequence")
    private String frequence;

    @Column(name = "nb_cycles_total")
    private Integer nbCycles;

    @Column(name = "description")
    private String description;

    @Column(name = "jour_cotisation")
    private Integer jourCotisation;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutTontine statut;

    @ManyToOne
    @JoinColumn(name = "gestionnaire_id")
    private Utilisateur gestionnaire;
}
