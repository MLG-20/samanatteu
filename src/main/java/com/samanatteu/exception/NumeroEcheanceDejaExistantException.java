package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class NumeroEcheanceDejaExistantException extends SamanatteuException{
    public NumeroEcheanceDejaExistantException(Integer numeroEcheance, Long pretId){
        super("L'échéance n°" + numeroEcheance + " existe déjà pour le prêt " + pretId + ".", HttpStatus.CONFLICT);
    }
}
