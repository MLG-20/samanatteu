package com.samanatteu.entity.pret;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "membre_id")
    private Utilisateur membre;

    @ManyToOne
    @JoinColumn(name = "tontine_id")
    private Tontine tontine;

    @Column(name = "type")
    private String type;

    @Column(name = "montant")
    private BigDecimal montant;

    @Column(name = "sens")
    private String sens;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "description")
    private String description;

    @Column(name = "date_transaction")
    private LocalDateTime dateTransaction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
