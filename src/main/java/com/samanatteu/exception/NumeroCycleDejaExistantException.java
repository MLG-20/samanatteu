package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class NumeroCycleDejaExistantException extends SamanatteuException {
    public NumeroCycleDejaExistantException(Integer numeroCycle, Long tontineId){
        super("Le cycle n°" + numeroCycle + " existe déjà pour la tontine " + tontineId + ".", HttpStatus.CONFLICT);
    }
}
