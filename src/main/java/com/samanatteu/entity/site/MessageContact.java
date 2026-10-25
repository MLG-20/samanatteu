package com.samanatteu.entity.site;

import com.samanatteu.entity.commun.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// Un message envoyé par le formulaire de la page Contact.
//
// L'auteur est un VISITEUR (pas forcément un compte) : le message n'est
// rattaché à aucun utilisateur. Il laisse un téléphone ou un email (au moins
// l'un des deux) pour qu'on puisse lui répondre. L'ADMIN lit les messages
// depuis son back-office ; createdAt (Auditable) donne la date d'envoi.
@Entity
@Table(name = "message_contact")
@Getter
@Setter
public class MessageContact extends Auditable {

    // Constructeur sans argument exigé par JPA/Hibernate.
    // Écrit à la main plutôt que Lombok @NoArgsConstructor : sur ce JDK, Lombok
    // produit parfois un constructeur VIDE (sans appel à super()), et la JVM
    // refuse alors de charger la classe.
    public MessageContact() {
        super();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "email")
    private String email;

    @Column(name = "sujet", nullable = false, length = 150)
    private String sujet;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    // L'ADMIN le marque lu (ou non lu) depuis son back-office.
    @Column(name = "lu", nullable = false)
    private Boolean lu;
}
