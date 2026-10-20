package com.samanatteu.entity.abonnement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Tests de la règle "sans plafond" d'une formule.
//
// 0 = illimité. C'est une convention, pas un null : avec un null, chaque
// appelant devrait tester "si la valeur est négative/nulle", et oublierait.
class FormuleTest {

    private Formule formule(int nbTontinesMax) {
        Formule formule = new Formule();
        formule.setNom("TEST");
        formule.setNbTontinesMax(nbTontinesMax);
        return formule;
    }

    // Règle : un quota de 0 veut dire "sans plafond".
    @Test
    void estIllimitee_quotaZero_estVrai() {
        assertTrue(formule(0).estIllimitee());
    }

    // Règle : un quota positif = une limite.
    @Test
    void estIllimitee_quotaPositif_estFaux() {
        assertFalse(formule(10).estIllimitee());
    }
}
