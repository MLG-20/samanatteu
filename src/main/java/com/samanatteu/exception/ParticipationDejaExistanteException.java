package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class ParticipationDejaExistanteException extends SamanatteuException{
    public ParticipationDejaExistanteException(){
        super("Ce membre participe déjà à cette tontine", HttpStatus.CONFLICT);
    }
    
}
