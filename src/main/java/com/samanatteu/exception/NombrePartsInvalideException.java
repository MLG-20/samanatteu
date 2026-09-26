package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class NombrePartsInvalideException extends SamanatteuException{
    public NombrePartsInvalideException(){
        super("Le nombre de part doit être au moins 1.", HttpStatus.BAD_REQUEST);
    }
    
}
