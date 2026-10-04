package com.samanatteu.exception.auth;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// 400 et non 401 : l'utilisateur est bien connecté, il s'est trompé dans un champ.
// Un 401 ferait croire au front-end que la session est perdue.
public class AncienMotDePasseIncorrectException extends SamanatteuException {
    public AncienMotDePasseIncorrectException() {
        super("L'ancien mot de passe est incorrect.", HttpStatus.BAD_REQUEST);
    }
}
