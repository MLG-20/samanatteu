package com.samanatteu.entity.cotisation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.enums.cotisation.ModePaiementCotisation;
import com.samanatteu.enums.cotisation.StatutCotisation;

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
@Table(name = "cotisation")
@Getter
@Setter
@NoArgsConstructor

public class Cotisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participation_id")
    private Participation participation;

    @ManyToOne
    @JoinColumn(name = "cycle_id")
    private Cycle cycle;

    @Column(name = "montant_du")
    private BigDecimal montantDu;

    @Column(name = "montant_paye")
    private BigDecimal montantPaye;

    // Part de la caisse de prêts due pour ce cycle (copie de
    // tontine.montantCaissePret à l'ouverture du cycle).
    @Column(name = "montant_caisse_du")
    private BigDecimal montantCaisseDu;

    // Ce qui a déjà été versé à la caisse de prêts sur cette cotisation.
    @Column(name = "montant_caisse_paye")
    private BigDecimal montantCaissePaye;

    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement")
    private ModePaiementCotisation modePaiement;

    @Column(name = "reference")
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutCotisation statut;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
