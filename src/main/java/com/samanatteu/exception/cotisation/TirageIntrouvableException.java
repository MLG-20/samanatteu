package com.samanatteu.exception.cotisation;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class TirageIntrouvableException extends SamanatteuException{
    public TirageIntrouvableException() {
        super("Tirage introuvable.", HttpStatus.NOT_FOUND);
    }
}
