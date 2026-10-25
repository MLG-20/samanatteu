package com.samanatteu.service.utilisateur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.samanatteu.exception.utilisateur.TelephoneInvalideException;

// Test du point unique pour le téléphone.
//
// C'est un service Spring : @Value est résolu par Spring. Ici on teste donc
// la classe seule, en lui donnant l'indicatif — le câblage Spring (@Value sur
// samanatteu.pays-defaut) est vérifié par le démarrage de l'application.
class TelephoneServiceTest {

    // Règle : une saisie en format local devient de l'E.164.
    @Test
    void normaliser_convertitLeFormatLocal() {
        TelephoneService service = new TelephoneService("+221");

        assertEquals("+221771234566", service.normaliser("771234566"));
    }

    // Règle : le 0 national est retiré.
    @Test
    void normaliser_retireLeZeroNational() {
        TelephoneService service = new TelephoneService("+221");

        assertEquals("+221771234566", service.normaliser("0771234566"));
    }

    // Règle : une saisie déjà internationale ne bouge pas (idempotence).
    @Test
    void normaliser_estIdempotent() {
        TelephoneService service = new TelephoneService("+221");

        assertEquals("+221771234566", service.normaliser("+221771234566"));
    }

    // Règle : un numéro illisible est refusé (400 via TelephoneInvalideException).
    // Le bug trouvé à la connexion : "77 000 00 90" était refusé alors que le
    // numéro est juste. Espaces, points et tirets sont des séparateurs humains.
    @Test
    void normaliser_retireLesSeparateurs() {
        TelephoneService service = new TelephoneService("+221");

        assertEquals("+221771234566", service.normaliser("77 123 45 66"));
        assertEquals("+221771234566", service.normaliser("77.123.45.66"));
        assertEquals("+221771234566", service.normaliser("77-123-45-66"));
        assertEquals("+221771234566", service.normaliser("+221 77 123 45 66"));
    }

    @Test
    void normaliser_refuseUnNumeroIllisible() {
        TelephoneService service = new TelephoneService("+221");

        assertThrows(TelephoneInvalideException.class, () -> service.normaliser("abcdefgh"));
    }

    // Règle : l'indicatif est celui de la plateforme, et il est lisible.
    @Test
    void indicatifParDefaut_renvoieLaValeurFournie() {
        TelephoneService service = new TelephoneService("+225");

        assertEquals("+225", service.indicatifParDefaut());
    }
}
