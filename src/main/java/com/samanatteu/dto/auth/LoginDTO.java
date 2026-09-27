package com.samanatteu.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginDTO {
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
