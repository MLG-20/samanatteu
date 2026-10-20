package com.samanatteu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Le point unique qui sait quel indicatif pays utiliser.
 *
 * <p>Aujourd'hui, c'est un réglage de la plateforme ({@code samanatteu.pays-defaut}). Demain, dans
 * un SaaS multi-pays, ce sera le pays du tenant (l'abonnement). Pour que ce jour-là ne coûte rien,
 * AUCUN appelant ne lit la configuration : ils passent tous par ici.
 *
 * <p>Même principe que {@code UtilisateurConnecte} (« un seul point de lecture de l'utilisateur
 * connecté »), appliqué au pays.
 */
@Component
public class IndicatifPays {

    // L'indicatif, avec son "+" : "+221". @Value : la valeur vient de la
    // configuration (application.yaml, ou .env via ${...}).
    private final String indicatif;

    public IndicatifPays(@Value("${samanatteu.pays-defaut}") String indicatif) {
        this.indicatif = indicatif;
    }

    /** L'indicatif à utiliser pour convertir un numéro local : "+221". */
    public String indicatifParDefaut() {
        return indicatif;
    }
}
