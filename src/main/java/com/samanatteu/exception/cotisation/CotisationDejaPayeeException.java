package com.samanatteu.exception.cotisation;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class CotisationDejaPayeeException extends SamanatteuException {
    public CotisationDejaPayeeException() {
        super("Cette cotisation est déjà entièrement payée.", HttpStatus.CONFLICT);
    }
}
