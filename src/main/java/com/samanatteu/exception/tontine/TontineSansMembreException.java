package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class TontineSansMembreException extends SamanatteuException {
    public TontineSansMembreException() {
        super("Impossible d'activer une tontine sans membre : inscrivez au moins un membre avant.",
                HttpStatus.CONFLICT);
    }
}
