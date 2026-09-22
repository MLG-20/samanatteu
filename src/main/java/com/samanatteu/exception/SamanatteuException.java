package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

//abstract empêche l'instanciation directe
public abstract class SamanatteuException extends RuntimeException {
    //Déclaration de l'attribut
    private final HttpStatus statut;

    // Contructeur protected restreint l'usage du constructeur aux seules classes filles.
    protected SamanatteuException(String message, HttpStatus statut){
        super(message);
        this.statut = statut;
    }

    // La méthode 
    public HttpStatus getStatut(){
        return statut;
    }
}
