package com.samanatteu.exception.notification;

import org.springframework.http.HttpStatus;

import com.samanatteu.exception.SamanatteuException;

public class EnvoiNotificationEchoueException extends SamanatteuException{
    public EnvoiNotificationEchoueException(Long destinataireId){
        super("L'envoi de la notification au destinataire " + destinataireId + " a échoué.", HttpStatus.BAD_GATEWAY);
    }
}
