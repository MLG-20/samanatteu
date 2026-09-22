package com.samanatteu.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.samanatteu.enums.StatutEcheancePret;

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
@Table (name = "echeance_pret")
@Getter 
@Setter 
@NoArgsConstructor 
public class EcheancePret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne 
    @JoinColumn (name = "pret_id")
    private Pret pret;

    @Column (name = "numero_echeance")
    private Integer numeroEcheance;

    @Column (name = "montant_du")
    private BigDecimal montantDu;

    @Column (name = "montant_paye")
    private BigDecimal montantPaye;

    @Column (name = "date_echeance")
    private LocalDate dateEcheance;

    @Column (name = "date_paiement")
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column (name = "statut")
    private StatutEcheancePret statut;
}

