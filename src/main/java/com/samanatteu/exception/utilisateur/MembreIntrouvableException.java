package com.samanatteu.exception.utilisateur;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class MembreIntrouvableException extends  SamanatteuException{
    public MembreIntrouvableException(){
        super("Membre introuvable", HttpStatus.NOT_FOUND);
    }
}
