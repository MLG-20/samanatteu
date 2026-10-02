package com.samanatteu.enums.notification;

// Pourquoi on écrit au membre (CDC v1 §3.8 + invitations). Enum plutôt que String :
// une faute de frappe ne compile plus. Noms ≤ 20 caractères (colonne varchar(20)).
public enum TypeNotification {
    BIENVENUE, INVITATION, PAIEMENT_CONFIRME, RESULTAT_TIRAGE, RAPPEL_COTISATION, RAPPEL_ECHEANCE
}
