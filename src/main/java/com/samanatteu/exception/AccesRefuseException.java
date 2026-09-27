package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

// 403 : la personne est connectée mais n'a pas le droit d'agir sur cette ressource
// (profil d'un autre, tontine d'un autre gestionnaire, création d'un ADMIN...).
public class AccesRefuseException extends SamanatteuException {
    public AccesRefuseException(){
        super("Vous n'avez pas le droit d'effectuer cette action sur cette ressource.", HttpStatus.FORBIDDEN);
    }
}
