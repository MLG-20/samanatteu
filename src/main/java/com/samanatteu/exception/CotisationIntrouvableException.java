package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class CotisationIntrouvableException extends SamanatteuException {
    public CotisationIntrouvableException() {
        super("Cotisation introuvable.", HttpStatus.NOT_FOUND);
    }
}
