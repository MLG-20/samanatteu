package com.samanatteu.exception.cotisation;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class CotisationIntrouvableException extends SamanatteuException {
    public CotisationIntrouvableException() {
        super("Cotisation introuvable.", HttpStatus.NOT_FOUND);
    }
}
