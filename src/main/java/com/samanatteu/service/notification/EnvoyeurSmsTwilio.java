package com.samanatteu.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

// L'implémentation réelle : envoie le SMS par l'API Twilio.
// L'interface EnvoyeurSms reste le contrat : les services ne changent pas.
// @ConditionalOnProperty : cette classe n'existe que si
// samanatteu.sms.twilio.enabled=true. Sinon, EnvoyeurSmsConsole prend le relais
// (l'application démarre sans compte Twilio). Les deux ne coexistent jamais.
@Component
@ConditionalOnProperty(name = "samanatteu.sms.twilio.enabled", havingValue = "true")
public class EnvoyeurSmsTwilio implements EnvoyeurSms {

    private static final Logger logger = LoggerFactory.getLogger(EnvoyeurSmsTwilio.class);

    // Le client Twilio : construit par TwilioConfig à partir des identifiants
    // du compte (dans le .env). Injecté, donc testable.
    private final TwilioRestClient client;

    // Le numéro qui envoie les SMS, au format E.164 : "+17372508034".
    private final String numeroExpediteur;

    public EnvoyeurSmsTwilio(TwilioRestClient client,
            @Value("${samanatteu.sms.twilio.from}") String numeroExpediteur) {
        this.client = client;
        this.numeroExpediteur = numeroExpediteur;
    }

    @Override
    public void envoyer(String telephone, String message) {
        // Le téléphone est déjà en E.164 (c'est comme ça qu'il est stocké) :
        // Twilio n'accepte rien d'autre, et aucune conversion n'est nécessaire.
        //
        // En cas d'échec, Twilio lève une ApiException, qui est une
        // RuntimeException : elle remonte telle quelle, et NotificationService
        // l'attrape pour marquer la notification ECHEC (sans annuler le
        // paiement ni le tirage qui l'ont déclenchée).
        Message.creator(
                new PhoneNumber(telephone),
                new PhoneNumber(numeroExpediteur),
                message)
                .create(client);
        logger.info("SMS envoyé à {} via Twilio", telephone);
    }
}
