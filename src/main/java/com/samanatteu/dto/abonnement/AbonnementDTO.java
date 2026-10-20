package com.samanatteu.dto.abonnement;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

// L'abonnement d'une gestionnaire, vu de l'extérieur.
//
// Les quotas et le prix viennent de la FORMULE (une donnée que l'ADMIN
// modifie) : les renvoyer permet à l'écran d'afficher "3 tontines sur 10"
// sans connaître les règles de la plateforme.
//
// L'IDENTITÉ de la cliente (nom, téléphone, email) est aussi incluse : c'est
// l'ADMIN qui consulte cette liste, et il doit savoir de qui il s'agit. On
// reste du côté commercial : jamais le contenu de ses tontines.
@Getter
@Setter
public class AbonnementDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public AbonnementDTO() {
        super();
    }

    private Long id;

    private Long utilisateurId;

    // Qui est la cliente : c'est l'ADMIN qui regarde cette liste.
    private String nom;

    private String prenom;

    private String telephone;

    private String email;

    // La formule : son identifiant (pour l'éditer) et son nom (pour l'afficher).
    private Long formuleId;

    private String formule;

    // Le prix mensuel, en FCFA.
    private BigDecimal prixMensuel;

    private String statut;

    // Le statut seul ne suffit pas : un abonnement peut être "ACTIF" et
    // pourtant périmé (la date est calculée, pas stockée).
    private Boolean actif;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private Integer nbTontinesMax;

    private Integer nbMembresMax;

    private Integer smsParMois;

    private Integer smsEnvoyesMois;
}
