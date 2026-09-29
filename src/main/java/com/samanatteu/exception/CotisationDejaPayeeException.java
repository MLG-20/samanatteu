package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class CotisationDejaPayeeException extends SamanatteuException {
    public CotisationDejaPayeeException() {
        super("Cette cotisation est déjà entièrement payée.", HttpStatus.CONFLICT);
    }
}
