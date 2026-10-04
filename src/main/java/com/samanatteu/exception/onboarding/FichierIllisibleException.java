package com.samanatteu.exception.onboarding;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

// 400 : le fichier envoyé (CSV ou Excel) n'a pas pu être lu, c'est au client
// d'en renvoyer un bon. Lancée par les deux lecteurs de l'import.
public class FichierIllisibleException extends SamanatteuException{
    public FichierIllisibleException(){
        super("Le fichier est illisible : envoyez un fichier CSV ou Excel (.xlsx) valide.", HttpStatus.BAD_REQUEST);
    }
}
