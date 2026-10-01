package com.samanatteu.entity.tontine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.samanatteu.enums.tontine.StatutCycle;

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
@Table(name = "cycle")
@Getter
@Setter
@NoArgsConstructor
public class Cycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @Column(name = "numero_cycle")
    private Integer numeroCycle;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "date_fin_reelle")
    private LocalDate dateFinReelle;

    @Column(name = "montant_attendu")
    private BigDecimal montantAttendu;

    @Column(name = "montant_collecte")
    private BigDecimal montantCollecte;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutCycle statut;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
