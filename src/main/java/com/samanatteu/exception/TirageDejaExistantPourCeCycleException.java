package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class TirageDejaExistantPourCeCycleException extends SamanatteuException{
    public TirageDejaExistantPourCeCycleException(Long cycleId){
        super("Un tirage existe déjà pour le cycle " + cycleId + ".", HttpStatus.CONFLICT);//erreur 409
    }
}
