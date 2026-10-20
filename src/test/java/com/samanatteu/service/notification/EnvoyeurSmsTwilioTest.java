package com.samanatteu.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;

// Tests de l'envoi de SMS par Twilio.
//
// L'appel à l'API est STATIQUE (Message.creator(...)) : on ne peut pas injecter
// un faux dedans. On le mocke donc statiquement (mockStatic), ce que permet le
// mock-maker inline de Mockito 5.
//
// Le TwilioRestClient, lui, est injecté : c'est un vrai objet, on n'a pas besoin
// de le mocker (aucun appel HTTP n'est fait : c'est le MessageCreator mocké qui
// reçoit le client).
class EnvoyeurSmsTwilioTest {

    private final TwilioRestClient client = new TwilioRestClient.Builder("AC-test", "token-test").build();

    private EnvoyeurSmsTwilio envoyeur(String numeroExpediteur) {
        return new EnvoyeurSmsTwilio(client, numeroExpediteur);
    }

    // Règle : le SMS part vers le bon numéro, avec le bon message, et depuis le
    // numéro d'expédition configuré.
    @Test
    void envoyer_appelleTwilioAvecLeBonDestinataireEtLeBonMessage() {
        MessageCreator createur = mock(MessageCreator.class);

        try (MockedStatic<Message> message = mockStatic(Message.class)) {
            message.when(() -> Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class)))
                    .thenReturn(createur);

            envoyeur("+17372508034").envoyer("+221771234566", "Votre code est 123456");

            // Le destinataire est le numéro du membre, au format E.164.
            org.mockito.ArgumentCaptor<PhoneNumber> destinataire =
                    org.mockito.ArgumentCaptor.forClass(PhoneNumber.class);
            org.mockito.ArgumentCaptor<PhoneNumber> expediteur =
                    org.mockito.ArgumentCaptor.forClass(PhoneNumber.class);
            org.mockito.ArgumentCaptor<String> texte = org.mockito.ArgumentCaptor.forClass(String.class);
            // create(client) : c'est l'appel qui envoie vraiment.
            verify(createur).create(client);
            message.verify(() -> Message.creator(destinataire.capture(), expediteur.capture(), texte.capture()));

            assertEquals("+221771234566", destinataire.getValue().getEndpoint());
            assertEquals("+17372508034", expediteur.getValue().getEndpoint());
            assertEquals("Votre code est 123456", texte.getValue());
        }
    }

    // Règle : une panne de l'API Twilio remonte en RuntimeException. C'est
    // voluntary : NotificationService l'attrape pour marquer la notification
    // ECHEC, sans annuler le paiement qui l'a déclenchée.
    @Test
    void envoyer_unePanneTwilio_remonteEnRuntimeException() {
        MessageCreator createur = mock(MessageCreator.class);
        Mockito.doThrow(new ApiException("numéro non crédité"))
                .when(createur).create(Mockito.any(TwilioRestClient.class));

        try (MockedStatic<Message> message = mockStatic(Message.class)) {
            message.when(() -> Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class)))
                    .thenReturn(createur);

            assertThrows(ApiException.class,
                    () -> envoyeur("+17372508034").envoyer("+221771234566", "Message"));
        }
    }
}
