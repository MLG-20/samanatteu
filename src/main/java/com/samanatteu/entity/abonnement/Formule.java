package com.samanatteu.entity.abonnement;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Une formule d'abonnement : nom, quotas et prix.
 *
 * <p>C'est une DONNÉE, pas une constante : l'ADMIN la modifie depuis son dashboard (quota,
 * prix) sans redéployer l'application. C'est pour ça qu'elle est en table et pas dans un enum.
 */
@Entity
@Table(name = "formule")
@Getter
@Setter
public class Formule {

    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : sur ce JDK, Lombok
    // produit parfois un constructeur VIDE (sans appel à super()), et la JVM
    // refuse alors de charger la classe.
    public Formule() {
        super();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "DECOUVERTE", "PRO", "MAX"... : le nom affiché, modifiable par l'ADMIN.
    @Column(name = "nom", nullable = false, length = 30)
    private String nom;

    // 0 = sans plafond. Les quotas sont des entiers : pas de devise.
    @Column(name = "nb_tontines_max", nullable = false)
    private Integer nbTontinesMax;

    @Column(name = "nb_membres_max", nullable = false)
    private Integer nbMembresMax;

    @Column(name = "sms_par_mois", nullable = false)
    private Integer smsParMois;

    // Prix mensuel en FCFA. 0 = "sur devis".
    @Column(name = "prix_mensuel", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixMensuel;

    // Une formule retirée de la vente reste en base (les abonnements existants la
    // référencent), mais on ne peut plus la choisir pour un nouvel abonnement.
    @Column(name = "active", nullable = false)
    private Boolean active;

    // 0 = sans plafond : la formule ne limite rien.
    public boolean estIllimitee() {
        return nbTontinesMax != null && nbTontinesMax == 0;
    }
}
