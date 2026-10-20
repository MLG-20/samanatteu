package com.samanatteu.exception.utilisateur;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class TelephoneInvalideException extends SamanatteuException {
    public TelephoneInvalideException(String telephone) {
        super("Le numéro " + telephone + " n'est pas valide.", HttpStatus.BAD_REQUEST);
    }
}
