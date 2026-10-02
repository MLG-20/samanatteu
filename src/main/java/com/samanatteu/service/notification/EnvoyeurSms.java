package com.samanatteu.service.notification;

// Un contrat : « je sais envoyer un SMS », sans dire comment. Les services ne
// connaissent que cette interface ; on change de fournisseur (console, Orange,
// Twilio) en changeant d'implémentation, sans toucher aux services.
public interface EnvoyeurSms {
    // Pas de corps : chaque implémentation écrit le sien.
    void envoyer(String telephone, String message);
}
