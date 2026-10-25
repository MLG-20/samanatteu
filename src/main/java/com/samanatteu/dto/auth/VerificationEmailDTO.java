package com.samanatteu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// Le jeton lu dans le lien cliqué (/verifier-email/{jeton}) : la page du front
// le renvoie tel quel. C'est la seule preuve : la route est publique.
@Getter
@Setter
public class VerificationEmailDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public VerificationEmailDTO() {
        super();
    }

    @NotBlank
    private String jeton;
}
