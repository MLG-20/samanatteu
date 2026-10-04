package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class FichierVideException extends SamanatteuException {
    public FichierVideException() {
        super("Le fichier ne contient aucun membre : ajoutez au moins une ligne sous l'en-tête.",
                HttpStatus.BAD_REQUEST);
    }
}
