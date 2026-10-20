package com.samanatteu.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

import com.samanatteu.exception.utilisateur.TelephoneInvalideException;

/**
 * Un numéro de téléphone, stocké en E.164 (+221771234566).
 *
 * <p>Objet valeur : il ne contient que ses deux parties (code pays et numéro national), il est
 * immuable, et il ne peut pas exister dans un état invalide (constructeur privé + fabriques qui
 * valident). Deux numéros égaux selon {@link #e164()} sont le même numéro.
 *
 * <p>Pourquoi deux champs et pas une seule chaîne : un E.164 ne dit pas où son code pays finit
 * ("+221771234566" peut se lire "+22 1771234566"). Il faut donc connaître le code pays pour
 * découper — d'où le paramètre des fabriques.
 */
public final class NumeroTelephone {

    // E.164 : + puis 1-9 (code pays) puis 7 à 14 chiffres.
    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    // Format local : 8 ou 9 chiffres, éventuellement avec le 0 national devant.
    private static final Pattern LOCAL = Pattern.compile("^0?\\d{8,9}$");

    // Code pays SANS le "+" : "221".
    private final String codePays;
    // Numéro national SANS le code pays : "771234566".
    private final String numeroNational;

    // Constructeur PRIVE : personne ne peut créer un NumeroTelephone invalide.
    private NumeroTelephone(String codePays, String numeroNational) {
        this.codePays = codePays;
        this.numeroNational = numeroNational;
    }

    /**
     * Reconstruit un numéro depuis un E.164 déjà connu.
     *
     * @param numero    le numéro complet, "+221771234566"
     * @param indicatif le code pays du contexte, "+221" : le numéro doit être de ce pays
     */
    public static NumeroTelephone e164(String numero, String indicatif) {
        if (numero == null || !E164.matcher(numero).matches()) {
            throw new TelephoneInvalideException(numero);
        }
        String codePays = codePays(indicatif);
        // Le numéro doit être de CE pays : sinon on ne sait pas le découper.
        if (!numero.startsWith("+" + codePays)) {
            throw new TelephoneInvalideException(numero);
        }
        return new NumeroTelephone(codePays, numero.substring(codePays.length() + 1));
    }

    /**
     * Construit un numéro depuis un format local, en lui ajoutant l'indicatif du pays.
     *
     * @param numeroLocal "771234566" ou "0771234566" (0 national accepté)
     * @param indicatif   le code pays, "+221"
     */
    public static NumeroTelephone local(String numeroLocal, String indicatif) {
        if (numeroLocal == null) {
            throw new TelephoneInvalideException(numeroLocal);
        }
        // Déjà international : on ne touche à rien (idempotence). Un numéro qui
        // commence par "+" ne peut pas être un format local (LOCAL n'accepte que
        // des chiffres) : c'est soit de l'E.164 valide, soit invalide.
        if (numeroLocal.startsWith("+")) {
            return e164(numeroLocal, indicatif);
        }
        if (!LOCAL.matcher(numeroLocal).matches()) {
            throw new TelephoneInvalideException(numeroLocal);
        }
        // Le 0 national ne fait pas partie du numéro international.
        String numeroNettoye = numeroLocal.startsWith("0")
                ? numeroLocal.substring(1)
                : numeroLocal;
        return new NumeroTelephone(codePays(indicatif), numeroNettoye);
    }

    // Le code pays, sans son "+" : "+221" → "221".
    private static String codePays(String indicatif) {
        if (indicatif == null || indicatif.isBlank()) {
            throw new TelephoneInvalideException(String.valueOf(indicatif));
        }
        return indicatif.replace("+", "").trim();
    }

    /** Le numéro au format international : c'est ce qu'on stocke en base. */
    public String e164() {
        return "+" + codePays + numeroNational;
    }

    /**
     * Deux numéros sont égaux s'ils ont le même code pays ET le même numéro national.
     *
     * <p>Un objet valeur se compare par sa VALEUR, pas par son identité : deux numéros saisis
     * différemment ("771234566" et "0771234566") sont le même numéro. Sans ce {@code equals}, un
     * test Mockito ou une comparaison en base les traiterait comme différents.
     */
    @Override
    public boolean equals(Object autre) {
        // Raccourci : la même instance est forcément égale à elle-même.
        if (this == autre) {
            return true;
        }
        // null ou un autre type : jamais égal (et on évite le ClassCastException).
        if (autre == null || getClass() != autre.getClass()) {
            return false;
        }
        NumeroTelephone numero = (NumeroTelephone) autre;
        return codePays.equals(numero.codePays) && numeroNational.equals(numero.numeroNational);
    }

    /**
     * Doit être COHÉRENT avec {@link #equals} : deux numéros égaux doivent avoir le même hashCode.
     * (Contrat de {@link Object} : sans cela, ils partageraient deux cases différentes d'un
     * {@code HashMap} ou d'un {@code HashSet}.)
     */
    @Override
    public int hashCode() {
        return Objects.hash(codePays, numeroNational);
    }

    /**
     * Le numéro pour l'écran : "+221 77 123 45 66".
     *
     * <p>Format national sénégalais : 2 chiffres d'opérateur, puis 3-2-2. C'est SPÉCIFIQUE à
     * chaque pays (France : 1-2-2-2-2) : le jour où la plateforme sert plusieurs pays, ce
     * découpage doit devenir une donnée, pas du code.
     */
    public String affichage() {
        StringBuilder groupes = new StringBuilder();
        // 2 chiffres d'opérateur : "77".
        groupes.append(numeroNational.substring(0, 2)).append(' ');
        // Le reste (7 chiffres) en 3-2-2 : "123 45 66".
        String reste = numeroNational.substring(2);
        if (reste.length() >= 3) {
            groupes.append(reste, 0, 3).append(' ');
            String dernier = reste.substring(3);
            for (int i = 0; i < dernier.length(); i += 2) {
                if (i > 0) {
                    groupes.append(' ');
                }
                groupes.append(dernier, i, Math.min(i + 2, dernier.length()));
            }
        } else {
            groupes.append(reste);
        }
        return "+" + codePays + " " + groupes.toString().trim();
    }

    /**
     * Le numéro est-il acceptable ? Question, pas action : jamais d'exception.
     * Accepte le format E.164 et le format local.
     */
    public static boolean estValide(String numero) {
        if (numero == null) {
            return false;
        }
        return E164.matcher(numero).matches() || LOCAL.matcher(numero).matches();
    }
}
