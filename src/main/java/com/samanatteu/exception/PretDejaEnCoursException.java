package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class PretDejaEnCoursException extends SamanatteuException{
    public PretDejaEnCoursException(Long membreId){
        super("Le membre " + membreId + " a déjà un prêt en cours.", HttpStatus.CONFLICT);
    }
}
