package com.samanatteu.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.samanatteu.exception.utilisateur.TelephoneInvalideException;

// Tests de l'objet valeur NumeroTelephone.
//
// Un objet valeur se teste par ses CAS LIMITES : chaque règle de la note de
// conception a droit à un test. Règle d'or : « un test qui échoue doit dire
// quelle règle a été cassée » — le nom du test porte la règle.
class NumeroTelephoneTest {

    private static final String SENEGAL = "+221";
    private static final String COTE_D_IVOIRE = "+225";

    // Règle : un numéro local devient de l'E.164 avec l'indicatif du pays.
    @Test
    void local_ajouteLE164() {
        assertEquals("+221771234566", NumeroTelephone.local("771234566", SENEGAL).e164());
    }

    // Règle : le 0 national ne fait pas partie du numéro international.
    @Test
    void local_retireLEZeroNational() {
        assertEquals("+221771234566", NumeroTelephone.local("0771234566", SENEGAL).e164());
    }

    // Règle d'idempotence : un numéro déjà international ne bouge pas.
    // (Sinon un membre qui tape son numéro en international se fait rejeter.)
    @Test
    void local_dejaInternational_resteInchange() {
        assertEquals("+221771234566", NumeroTelephone.local("+221771234566", SENEGAL).e164());
    }

    // Règle : l'indicatif est fourni avec son "+", la classe s'en charge.
    @Test
    void local_indicatifSansPlus_estAccepte() {
        assertEquals("+221771234566", NumeroTelephone.local("771234566", "221").e164());
    }

    // Règle : un autre pays donne un autre numéro, sans confusion possible.
    // C'est tout l'intérêt de l'E.164 en base.
    @Test
    void local_autrePays_neDonnePasLeMemeNumero() {
        assertEquals("+225771234566", NumeroTelephone.local("771234566", COTE_D_IVOIRE).e164());
    }

    // Règle : e164() reconstruit un numéro depuis un international connu.
    @Test
    void e164_accepteUnNumeroInternational() {
        assertEquals("+225771234566", NumeroTelephone.e164("+225771234566", COTE_D_IVOIRE).e164());
    }

    // Règle : un numéro d'un AUTRE pays que le contexte est refusé : on ne
    // peut pas le découper correctement.
    @Test
    void e164_numeroDUnAutrePays_estRefuse() {
        assertThrows(TelephoneInvalideException.class,
                () -> NumeroTelephone.e164("+225771234566", SENEGAL));
    }

    // Règle : e164() refuse ce qui n'est pas international (pas de "+").
    @Test
    void e164_refuseUnNumeroLocal() {
        assertThrows(TelephoneInvalideException.class,
                () -> NumeroTelephone.e164("771234566", SENEGAL));
    }

    // Règle : le null est refusé, pas un NPE.
    @Test
    void e164_refuseLeNull() {
        assertThrows(TelephoneInvalideException.class, () -> NumeroTelephone.e164(null, SENEGAL));
    }

    // Règle : un numéro trop court (moins de 7 chiffres après le code pays)
    // ou trop long (plus de 14) est refusé.
    @ParameterizedTest
    @ValueSource(strings = { "+2217712", "+22177123456789012" })
    void e164_refuseLesLongueursImpossibles(String numero) {
        assertThrows(TelephoneInvalideException.class,
                () -> NumeroTelephone.e164(numero, SENEGAL));
    }

    // Règle : l'indicatif ne peut pas commencer par 0 (E.164 l'interdit).
    @Test
    void e164_refuseUnIndicatifCommencantParZero() {
        assertThrows(TelephoneInvalideException.class,
                () -> NumeroTelephone.e164("+021771234566", "+021"));
    }

    // Règle : un numéro local illisible est refusé (lettres, trop court).
    @ParameterizedTest
    @ValueSource(strings = { "abcdefgh", "12345", "771234567890" })
    void local_refuseUnNumeroIllisible(String numero) {
        assertThrows(TelephoneInvalideException.class,
                () -> NumeroTelephone.local(numero, SENEGAL));
    }

    // Règle : estValide() est une QUESTION (pas une action) : jamais d'exception.
    @ParameterizedTest
    @CsvSource({
            "+221771234566, true",
            "771234566, true",
            "0771234566, true",
            "abcdefgh, false",
            "12345, false",
            "'', false"
    })
    void estValide_repondSansLever(String numero, boolean attendu) {
        assertEquals(attendu, NumeroTelephone.estValide(numero));
    }

    // Règle : estValide(null) = false, pas d'exception.
    @Test
    void estValide_nullEstFalse() {
        assertFalse(NumeroTelephone.estValide(null));
    }

    // Règle : un numéro valide est reconnu.
    @ParameterizedTest
    @ValueSource(strings = { "+221771234566", "771234566", "0771234566" })
    void estValide_accepteLesFormatsAttendus(String numero) {
        assertTrue(NumeroTelephone.estValide(numero));
    }

    // Règle d'affichage : indicatif séparé, puis format national sénégalais
    // 2 + 3 + 2 + 2 : "+221 77 123 45 66".
    @Test
    void affichage_utiliseLeFormatNational() {
        assertEquals("+221 77 123 45 66", NumeroTelephone.e164("+221771234566", SENEGAL).affichage());
    }

    // Règle d'affichage : un numéro d'un autre pays s'affiche avec SON pays.
    @Test
    void affichage_gardeLE164SiLePaysDiffert() {
        assertEquals("+225 77 123 45 66", NumeroTelephone.e164("+225771234566", COTE_D_IVOIRE).affichage());
    }

    // Règle d'un objet valeur : deux numéros de même VALEUR sont égaux, même
    // saisis différemment. C'est la preuve que la normalisation marche.
    @Test
    void deuxNumerosDeMemeValeur_sontEgaux() {
        NumeroTelephone a = NumeroTelephone.local("771234566", SENEGAL);
        NumeroTelephone b = NumeroTelephone.local("0771234566", SENEGAL);

        assertEquals(a, b);
        // Le contrat de Object l'exige : égaux ⇒ même hashCode.
        assertEquals(a.hashCode(), b.hashCode());
    }

    // Règle d'equals : un numéro n'est égal ni à null, ni à un autre type.
    @Test
    void equals_refuseNullEtAutreType() {
        NumeroTelephone numero = NumeroTelephone.local("771234566", SENEGAL);

        assertNotEquals(numero, null);
        assertNotEquals(numero, "+221771234566");
    }

    // Règle d'equals : deux pays différents ⇒ deux numéros différents, même
    // si les chiffres du numéro national coïncident.
    @Test
    void deuxPaysDifferents_neSontPasEgaux() {
        NumeroTelephone senegal = NumeroTelephone.local("771234566", SENEGAL);
        NumeroTelephone coteDIvoire = NumeroTelephone.local("771234566", COTE_D_IVOIRE);

        assertNotEquals(senegal, coteDIvoire);
    }
}
