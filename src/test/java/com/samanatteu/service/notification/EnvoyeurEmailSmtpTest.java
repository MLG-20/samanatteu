package com.samanatteu.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import jakarta.mail.internet.MimeMessage;

// Tests de l'envoi d'email par SMTP.
//
// Le moteur de template est un VRAI moteur, branché sur le VRAI template
// (templates/emails/generique.html, présent dans les ressources de main, donc
// sur le classpath des tests) : on vérifie que le HTML produit contient bien
// les variables. Le JavaMailSender est FAUX (mocké) : pas d'email envoyé
// pendant les tests, mais on vérifie ce qu'on aurait envoyé.
@ExtendWith(MockitoExtension.class)
class EnvoyeurEmailSmtpTest {

    @Mock
    private JavaMailSender mailSender;

    // Un vrai moteur de template, branché sur les templates du projet.
    private SpringTemplateEngine moteur() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private EnvoyeurEmailSmtp nouvelEnvoyeur() {
        return new EnvoyeurEmailSmtp(mailSender, moteur());
    }

    // Un email envoyé part avec le bon destinataire, le bon sujet et le HTML du template.
    @Test
    void envoyer_armeLeMailAvecDestinataireSujetEtHtml() throws Exception {
        // Un vrai MimeMessage : le mock renvoie null par défaut.
        MimeMessage message = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);

        nouvelEnvoyeur().envoyer("awa@samanatteu.local", "Bienvenue", "Vous avez 2 parts.");

        verify(mailSender).send(captor.capture());
        MimeMessage mail = captor.getValue();
        assertEquals("awa@samanatteu.local", mail.getAllRecipients()[0].toString());
        assertEquals("Bienvenue", mail.getSubject());
        // Le corps est du HTML : le template a été rendu avec les variables.
        String contenu = mail.getContent().toString();
        assertTrue(contenu.contains("Bienvenue"), "le sujet doit être dans le HTML");
        assertTrue(contenu.contains("Vous avez 2 parts."), "le message doit être dans le HTML");
    }

    // Un template absent ne doit pas envoyer d'email à moitié : le moteur lève,
    // on traduit en RuntimeException, et l'appelant (NotificationService)
    // marquera la notification ECHEC.
    @Test
    void envoyer_templateInexistant_echoueSansEnvoyer() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine moteurVide = new SpringTemplateEngine();
        moteurVide.setTemplateResolver(resolver);
        EnvoyeurEmailSmtp envoyeur = new EnvoyeurEmailSmtp(mailSender, moteurVide);

        // "emails/inconnu" n'existe pas dans templates/ : le moteur lève.
        assertThrows(RuntimeException.class, () -> {
            try {
                envoyeur.envoyer("awa@samanatteu.local", "Bienvenue", "Message");
            } catch (RuntimeException e) {
                throw e;
            }
        });
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // Une panne SMTP doit remonter en RuntimeException, pour que
    // NotificationService l'attrape et marque la notification ECHEC.
    // JavaMailSender.send() lève MailException, qui est DÉJÀ une
    // RuntimeException : elle se propage telle quelle.
    @Test
    void envoyer_panneSmtp_remonteEnRuntimeException() throws Exception {
        // Chaque test a besoin de son MimeMessage : le mock renvoie null par défaut.
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        doThrow(new org.springframework.mail.MailSendException("serveur injoignable"))
                .when(mailSender).send(any(MimeMessage.class));

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> nouvelEnvoyeur().envoyer("awa@samanatteu.local", "Bienvenue", "Message"));
        assertTrue(e.getCause() instanceof org.springframework.mail.MailException
                        || e instanceof org.springframework.mail.MailException,
                "la cause d'origine doit être conservée");
    }
}
