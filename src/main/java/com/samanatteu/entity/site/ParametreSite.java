package com.samanatteu.entity.site;

import com.samanatteu.entity.commun.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Les paramètres du site public : réseaux sociaux, contact, « À propos ».
//
// UNE SEULE LIGNE en base (id = 1, imposé par un CHECK dans la migration V11).
// C'est une DONNÉE que l'ADMIN modifie depuis son back-office, comme les
// formules : changer un numéro ou un texte ne demande pas de redéployer.
//
// Un champ null = rien à afficher (le réseau n'a pas d'icône dans le footer).
@Entity
@Table(name = "parametre_site")
@Getter
@Setter
public class ParametreSite extends Auditable {

    // L'identifiant de l'UNIQUE ligne.
    public static final Long ID_UNIQUE = 1L;

    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : sur ce JDK, Lombok
    // produit parfois un constructeur VIDE (sans appel à super()), et la JVM
    // refuse alors de charger la classe.
    public ParametreSite() {
        super();
    }

    // Pas de @GeneratedValue : l'id est toujours 1, fixé par la migration.
    @Id
    private Long id;

    // ---------- Les réseaux sociaux (URL complète) ----------

    @Column(name = "facebook")
    private String facebook;

    @Column(name = "instagram")
    private String instagram;

    @Column(name = "tiktok")
    private String tiktok;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "linkedin")
    private String linkedin;

    @Column(name = "youtube")
    private String youtube;

    @Column(name = "x")
    private String x;

    // ---------- La page Contact ----------

    @Column(name = "contact_telephone", length = 20)
    private String contactTelephone;

    @Column(name = "contact_whatsapp", length = 20)
    private String contactWhatsapp;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_adresse")
    private String contactAdresse;

    @Column(name = "contact_horaires")
    private String contactHoraires;

    // ---------- La page À propos ----------

    @Column(name = "apropos_accroche")
    private String aproposAccroche;

    @Column(name = "apropos_histoire", columnDefinition = "text")
    private String aproposHistoire;

    @Column(name = "apropos_mission", columnDefinition = "text")
    private String aproposMission;

    @Column(name = "apropos_valeurs", columnDefinition = "text")
    private String aproposValeurs;
}
