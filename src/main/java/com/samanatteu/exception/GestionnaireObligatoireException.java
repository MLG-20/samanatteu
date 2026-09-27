package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class GestionnaireObligatoireException extends SamanatteuException {
    public GestionnaireObligatoireException() {
        super("Un gestionnaire est obligatoire pour créer une tontine.", HttpStatus.BAD_REQUEST);
    }
}
