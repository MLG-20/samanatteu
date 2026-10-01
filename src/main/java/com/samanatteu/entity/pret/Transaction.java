package com.samanatteu.entity.pret;

import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.ModePaiement;
import com.samanatteu.enums.pret.SensTransaction;
import com.samanatteu.enums.pret.TypeTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    // STRING : on stocke le nom ("GAIN"), pas la position, qui changerait
    // si on réordonnait l'enum.
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TypeTransaction type;

    @Column(name = "montant")
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "sens")
    private SensTransaction sens;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement")
    private ModePaiement modePaiement;

    @Column(name = "reference")
    private String reference;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "description")
    private String description;

    @Column(name = "date_transaction")
    private LocalDateTime dateTransaction;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
