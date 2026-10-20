package com.samanatteu.exception.utilisateur;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// L'email est obligatoire pour ce compte : les gestionnaires (elles paient,
// on doit pouvoir leur facturer) et les administrateurs (ils gèrent la
// plateforme). Les membres, eux, l'ont toujours facultatif.
public class EmailObligatoireException extends SamanatteuException {
    public EmailObligatoireException(String role) {
        super("Un compte " + role + " doit avoir un email.", HttpStatus.BAD_REQUEST);
    }
}
