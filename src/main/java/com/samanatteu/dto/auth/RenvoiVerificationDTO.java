package com.samanatteu.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

// Le même identifiant qu'à la connexion (email OU téléphone) : le front renvoie
// ce que la gestionnaire vient de taper quand la connexion lui est refusée.
@Getter
@Setter
public class RenvoiVerificationDTO {
    // Constructeur sans argument : Jackson en a besoin pour deserialiser
    // le JSON en DTO. Ecrit a la main, pas genere par Lombok : sur ce JDK,
    // @NoArgsConstructor produit parfois un constructeur VIDE (sans appel a
    // super()), et la JVM refuse alors de charger la classe.
    public RenvoiVerificationDTO() {
        super();
    }

    private String email;

    private String telephone;

    @AssertTrue(message = "Il faut fournir un email ou un numéro de téléphone.")
    public boolean isIdentifiantFourni() {
        return email != null && !email.isBlank() ||
                telephone != null && !telephone.isBlank();
    }
}
