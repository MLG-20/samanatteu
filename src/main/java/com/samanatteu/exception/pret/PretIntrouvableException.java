package com.samanatteu.exception.pret;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class PretIntrouvableException extends SamanatteuException{
    public PretIntrouvableException() {
        super("Prêt introuvable.", HttpStatus.NOT_FOUND);
    }

}
