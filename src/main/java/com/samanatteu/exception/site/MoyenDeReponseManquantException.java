package com.samanatteu.exception.site;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// Un message de contact sans téléphone NI email : impossible d'y répondre.
public class MoyenDeReponseManquantException extends SamanatteuException {
    public MoyenDeReponseManquantException() {
        super("Indiquez un téléphone ou un email pour que nous puissions vous répondre.", HttpStatus.BAD_REQUEST);
    }
}
