package com.samanatteu.enums.abonnement;

// L'état que la PLATEFORME donne à un abonnement.
// L'expiration, elle, n'est pas un statut : elle se calcule sur la date
// (voir Abonnement.estActif()). Ici, on ne note que les décisions humaines.
public enum StatutAbonnement {
    ESSAI, // l'essai gratuit (30 jours) qui suit la vérification de l'email
    ACTIF, // l'abonnement court
    SUSPENDU, // la plateforme l'a suspendu (impayé, contrôle) : rien ne passe
    RESILIE // la cliente a résilié : c'est fini
}
