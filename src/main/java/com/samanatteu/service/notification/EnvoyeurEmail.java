package com.samanatteu.service.notification;

// Même contrat que EnvoyeurSms, pour l'email (CDC v1.1 : email EN PLUS du SMS).
// Le vrai envoi (Spring Mail + SMTP) sera une autre implémentation.
public interface EnvoyeurEmail {
    // sujet : un email a un objet, un SMS non (stocké dans la colonne titre).
    void envoyer(String email, String sujet, String message);

    // Le même email, avec un BOUTON d'action sous le message (ex. « Confirmer
    // mon adresse »). Le lien est aussi écrit en clair, pour les messageries
    // qui n'affichent pas les boutons.
    void envoyerAvecLien(String email, String sujet, String message, String libelleBouton, String lien);
}
