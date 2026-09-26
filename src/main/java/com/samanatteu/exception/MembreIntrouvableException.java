package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class MembreIntrouvableException extends  SamanatteuException{
    public MembreIntrouvableException(){
        super("Membre introuvable", HttpStatus.NOT_FOUND);
    }
}
