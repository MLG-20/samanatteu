package com.samanatteu.exception.pret;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class PretDejaEnCoursException extends SamanatteuException{
    public PretDejaEnCoursException(Long membreId){
        super("Le membre " + membreId + " a déjà un prêt en cours.", HttpStatus.CONFLICT);
    }
}
