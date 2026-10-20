package com.samanatteu.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// Test du point unique qui fournit l'indicatif pays.
//
// C'est un composant Spring : @Value est résolu par Spring. Ici on teste donc
// la classe seule, en lui donnant l'indicatif — le câblage Spring (@Value sur
// samanatteu.pays-defaut) est vérifié par le démarrage de l'application.
class IndicatifPaysTest {

    // L'indicatif est renvoyé tel quel : c'est l'appelant qui l'utilise.
    @Test
    void indicatifParDefaut_renvoieLaValeurFournie() {
        IndicatifPays indicatifPays = new IndicatifPays("+221");

        assertEquals("+221", indicatifPays.indicatifParDefaut());
    }

    // Un autre pays ne demande qu'une autre valeur : c'est tout le principe
    // de ce point unique (demain, la valeur viendra du tenant).
    @Test
    void indicatifParDefaut_suitLaConfiguration() {
        IndicatifPays indicatifPays = new IndicatifPays("+225");

        assertEquals("+225", indicatifPays.indicatifParDefaut());
    }
}
