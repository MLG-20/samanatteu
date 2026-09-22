package com.samanatteu.exception;

import org.springframework.http.HttpStatus;

public class EnvoiNotificationEchoueException extends SamanatteuException{
    public EnvoiNotificationEchoueException(Long destinataireId){
        super("L'envoi de la notification au destinataire " + destinataireId + " a échoué.", HttpStatus.BAD_GATEWAY);//erreur 502
    }
}
