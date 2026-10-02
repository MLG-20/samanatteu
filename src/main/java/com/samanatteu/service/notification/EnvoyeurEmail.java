package com.samanatteu.service.notification;

// Même contrat que EnvoyeurSms, pour l'email (CDC v1.1 : email EN PLUS du SMS).
// Le vrai envoi (Spring Mail + SMTP) sera une autre implémentation.
public interface EnvoyeurEmail {
    // sujet : un email a un objet, un SMS non (stocké dans la colonne titre).
    void envoyer(String email, String sujet, String message);
}
