package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

// Message volontairement générique ("Identifiants incorrects", sans dire si
// c'est l'email/téléphone OU le mot de passe qui est faux) : ne pas révéler à
// un attaquant si un compte existe ou non pour un identifiant donné.
public class IdentifiantsInvalidesException extends SamanatteuException {
    public IdentifiantsInvalidesException(){
        super("Identifiants incorrects",HttpStatus.UNAUTHORIZED);
    }
}
