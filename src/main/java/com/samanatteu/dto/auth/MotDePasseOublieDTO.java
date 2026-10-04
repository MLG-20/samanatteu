package com.samanatteu.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Le téléphone est tout ce que l'utilisateur sait encore de son compte,
// et c'est là que le code sera envoyé.
@Getter
@Setter
@NoArgsConstructor
public class MotDePasseOublieDTO {
    @NotBlank
    private String telephone;
}
