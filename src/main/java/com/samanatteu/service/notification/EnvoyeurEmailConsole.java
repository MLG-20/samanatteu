package com.samanatteu.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Implémentation provisoire : écrit l'email dans la console au lieu de l'envoyer.
// @Component : Spring la crée et l'injecte partout où un EnvoyeurEmail est demandé.
// @ConditionalOnProperty : elle n'est créée que si l'envoi SMTP est désactivé
// (samanatteu.mail.enabled absent ou false). Sinon, EnvoyeurEmailSmtp prend
// le relais. Les deux ne doivent JAMAIS coexister : Spring ne saurait pas
// lequel injecter.
@Component
@ConditionalOnProperty(name = "samanatteu.mail.enabled", havingValue = "false", matchIfMissing = true)
public class EnvoyeurEmailConsole implements EnvoyeurEmail {
    private static final Logger logger = LoggerFactory.getLogger(EnvoyeurEmailConsole.class);

    @Override
    public void envoyer(String email, String sujet, String message) {
        logger.info("EMAIL (simulé) à {} — {} : {}", email, sujet, message);
    }

    // Sans SMTP, le lien s'affiche dans les logs : on peut tester le parcours
    // (copier le lien dans le navigateur) sans boîte mail.
    @Override
    public void envoyerAvecLien(String email, String sujet, String message, String libelleBouton, String lien) {
        logger.info("EMAIL (simulé) à {} — {} : {} [{}] {}", email, sujet, message, libelleBouton, lien);
    }
}
