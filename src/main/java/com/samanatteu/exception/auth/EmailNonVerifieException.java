package com.samanatteu.exception.auth;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Bon mot de passe, mais email pas encore confirmé : connexion refusée.
//
// 403 et non 401 : c'est ce qui permet au front de reconnaître ce cas (401 =
// identifiants faux) et de proposer « Renvoyer l'email ». Levée APRÈS le contrôle
// du mot de passe : seul le titulaire du compte apprend qu'il existe.
public class EmailNonVerifieException extends SamanatteuException {
    public EmailNonVerifieException() {
        super("Confirmez d'abord votre adresse email : ouvrez le lien que nous vous avons envoyé.",
                HttpStatus.FORBIDDEN);
    }
}
