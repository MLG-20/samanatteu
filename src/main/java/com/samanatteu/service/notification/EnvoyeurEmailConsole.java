package com.samanatteu.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Implémentation provisoire : écrit l'email dans la console au lieu de l'envoyer.
// @Component : sans lui, Spring ne trouve aucun EnvoyeurEmail et l'appli ne démarre pas.
@Component
public class EnvoyeurEmailConsole implements EnvoyeurEmail {
    private static final Logger logger = LoggerFactory.getLogger(EnvoyeurEmailConsole.class);

    @Override
    public void envoyer(String email, String sujet, String message) {
        logger.info("EMAIL (simulé) à {} — {} : {}", email, sujet, message);
    }
}
