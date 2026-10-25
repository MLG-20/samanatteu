package com.samanatteu.dto.site;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

// Un message de contact, tel que l'ADMIN le voit dans son back-office.
@Getter
@Setter
public class MessageContactDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public MessageContactDTO() {
        super();
    }

    private Long id;

    private String nom;

    private String telephone;

    private String email;

    private String sujet;

    private String message;

    private Boolean lu;

    // La date d'envoi.
    private LocalDateTime createdAt;
}
