package com.samanatteu.service.utilisateur;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.samanatteu.valueobject.NumeroTelephone;

/**
 * Le point unique pour le numéro de téléphone.
 *
 * <p>Aucun service ne lit la configuration du pays : ils passent tous par ici. Aujourd'hui le pays
 * vient de la plateforme ({@code samanatteu.pays-defaut}) ; dans un SaaS multi-pays, il viendra
 * du tenant (l'abonnement). Ce jour-là, seul cet objet changera de source.
 *
 * <p>Même principe que {@code UtilisateurConnecte} (« un seul point de lecture de l'utilisateur
 * connecté »), appliqué au téléphone.
 */
@Service
public class TelephoneService {

    // L'indicatif, avec son "+" : "+221".
    private final String indicatif;

    public TelephoneService(@Value("${samanatteu.pays-defaut}") String indicatif) {
        this.indicatif = indicatif;
    }

    /**
     * Normalise une saisie en E.164 : "771234566" ou "0771234566" ou "+221771234566" donnent
     * tous "+221771234566".
     *
     * <p>Les séparateurs qu'un humain tape naturellement ("77 123 45 66", "77.123.45.66",
     * "77-123-45-66") sont retirés d'abord : sans ça, la connexion répondait « téléphone ou mot
     * de passe incorrect » à un numéro pourtant juste.
     *
     * <p>À n'utiliser qu'aux ENTRÉES (inscription, connexion, import, invitation). Un numéro
     * lu en base est déjà en E.164 : le re-normaliser serait inutile, et le refuserait s'il
     * appartient à un autre pays.
     */
    public String normaliser(String saisie) {
        String sansSeparateurs = saisie == null ? null : saisie.replaceAll("[\\s.\\-]", "");
        return NumeroTelephone.local(sansSeparateurs, indicatif).e164();
    }

    /** L'indicatif utilisé : "+221". */
    public String indicatifParDefaut() {
        return indicatif;
    }
}
