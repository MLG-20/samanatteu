package com.samanatteu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Le téléphone est tout ce que l'utilisateur sait encore de son compte,
// et c'est là que le code sera envoyé.
@Getter
@Setter
public class MotDePasseOublieDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public MotDePasseOublieDTO() {
        super();
    }

    @NotBlank
    private String telephone;
}
