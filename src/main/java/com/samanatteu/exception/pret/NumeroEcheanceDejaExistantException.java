package com.samanatteu.exception.pret;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class NumeroEcheanceDejaExistantException extends SamanatteuException{
    public NumeroEcheanceDejaExistantException(Integer numeroEcheance, Long pretId){
        super("L'échéance n°" + numeroEcheance + " existe déjà pour le prêt " + pretId + ".", HttpStatus.CONFLICT);
    }
}
