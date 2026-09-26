package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class TontineIntrouvableException extends SamanatteuException{
    public TontineIntrouvableException(){
        super("Tontine introuvable", HttpStatus.NOT_FOUND);
    }
}
