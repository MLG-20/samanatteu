package com.samanatteu.exception.cotisation;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class TirageDejaExistantPourCeCycleException extends SamanatteuException{
    public TirageDejaExistantPourCeCycleException(Long cycleId){
        super("Un tirage existe déjà pour le cycle " + cycleId + ".", HttpStatus.CONFLICT);
    }
}
