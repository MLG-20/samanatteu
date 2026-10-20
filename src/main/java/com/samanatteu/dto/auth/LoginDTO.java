package com.samanatteu.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public LoginDTO() {
        super();
    }

    private String email;

    private String telephone;

    @NotBlank
    @Size(min = 8)
    private String motDePasse;

    @AssertTrue(message = "Il faut fournir un email ou un numéro de téléphone.")
    public boolean isIdentifiantFourni() {
        return email != null && !email.isBlank() ||
                telephone != null && !telephone.isBlank();
    }
}
