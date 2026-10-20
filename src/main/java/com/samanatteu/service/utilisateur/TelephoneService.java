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
     * <p>À n'utiliser qu'aux ENTRÉES (inscription, connexion, import, invitation). Un numéro
     * lu en base est déjà en E.164 : le re-normaliser serait inutile, et le refuserait s'il
     * appartient à un autre pays.
     */
    public String normaliser(String saisie) {
        return NumeroTelephone.local(saisie, indicatif).e164();
    }

    /** L'indicatif utilisé : "+221". */
    public String indicatifParDefaut() {
        return indicatif;
    }
}
