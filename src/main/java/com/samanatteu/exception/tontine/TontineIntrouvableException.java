package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class TontineIntrouvableException extends SamanatteuException{
    public TontineIntrouvableException(){
        super("Tontine introuvable", HttpStatus.NOT_FOUND);
    }
}
