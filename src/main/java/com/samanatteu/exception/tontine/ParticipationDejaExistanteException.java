package com.samanatteu.exception.tontine;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class ParticipationDejaExistanteException extends SamanatteuException{
    public ParticipationDejaExistanteException(){
        super("Ce membre participe déjà à cette tontine", HttpStatus.CONFLICT);
    }
    
}
