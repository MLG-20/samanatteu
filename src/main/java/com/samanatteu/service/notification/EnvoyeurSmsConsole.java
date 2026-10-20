package com.samanatteu.service.notification;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Implémentation provisoire : écrit le SMS dans la console au lieu de l'envoyer.
// @Component : Spring la crée et l'injecte partout où un EnvoyeurSms est demandé.
// @ConditionalOnProperty : elle n'est créée que si l'envoi Twilio est désactivé
// (samanatteu.sms.twilio.enabled absent ou false). Sinon, EnvoyeurSmsTwilio
// prend le relais. Les deux ne doivent JAMAIS coexister : Spring ne saurait pas
// lequel injecter.
@Component
@ConditionalOnProperty(name = "samanatteu.sms.twilio.enabled", havingValue = "false", matchIfMissing = true)
public class EnvoyeurSmsConsole implements EnvoyeurSms {
    // SLF4J : le journal de Spring Boot (heure, niveau, classe), comme dans GlobalExceptionHandler.
    private static final Logger logger = LoggerFactory.getLogger(EnvoyeurSmsConsole.class);

    // @Override : le compilateur vérifie qu'on remplit bien la méthode du contrat.
    @Override
    public void envoyer(String telephone, String message) {
        logger.info("SMS (simulé) à {} : {}", telephone, message);
    }
}
