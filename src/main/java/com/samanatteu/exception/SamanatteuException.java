package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

// Chaque exception métier porte son propre code HTTP : GlobalExceptionHandler n'a pas à
// être modifié quand on en ajoute une. Abstraite : on lève toujours une fille précise.
public abstract class SamanatteuException extends RuntimeException {
    private final HttpStatus statut;

    protected SamanatteuException(String message, HttpStatus statut){
        super(message);
        this.statut = statut;
    }

    public HttpStatus getStatut(){
        return statut;
    }
}
