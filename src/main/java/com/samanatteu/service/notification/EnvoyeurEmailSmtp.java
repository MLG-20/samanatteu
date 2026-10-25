package com.samanatteu.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

// La VRAIE implémentation : envoie par SMTP. L'interface EnvoyeurEmail
// reste le contrat : les services ne changent pas.
// @ConditionalOnProperty : cette classe n'est créée QUE si
// samanatteu.mail.enabled=true. Préfixe "samanatteu." pour ne pas entrer en
// collision avec spring.mail.* (la config SMTP de Spring Boot).
// Attention : la propriété est lue à la RACINE de l'environnement, donc
// "samanatteu.mail.enabled" et non "spring.samanatteu.mail.enabled".
// Sans elle, l'implémentation console (EnvoyeurEmailConsole) prend le relais :
// l'application démarre quand même, sans serveur SMTP.
@Component
@ConditionalOnProperty(name = "samanatteu.mail.enabled", havingValue = "true")
public class EnvoyeurEmailSmtp implements EnvoyeurEmail {

    private static final Logger logger = LoggerFactory.getLogger(EnvoyeurEmailSmtp.class);

    // JavaMailSender : l'objet de Spring Boot qui parle SMTP.
    // Spring le crée tout seul à partir de spring.mail.* (application.yaml).
    private final JavaMailSender mailSender;

    // SpringTemplateEngine : rend le template HTML. Spring Boot le crée tout seul
    // à partir de spring-boot-starter-thymeleaf.
    private final SpringTemplateEngine templateEngine;

    public EnvoyeurEmailSmtp(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void envoyer(String email, String sujet, String message) {
        envoyerAvecLien(email, sujet, message, null, null);
    }

    // Un seul modèle pour les deux : le bouton n'apparaît que si un lien est
    // donné (th:if dans emails/generique.html).
    @Override
    public void envoyerAvecLien(String email, String sujet, String message, String libelleBouton, String lien) {
        try {
            Context contexte = new Context();
            contexte.setVariable("sujet", sujet);
            contexte.setVariable("message", message);
            contexte.setVariable("libelleBouton", libelleBouton);
            contexte.setVariable("lien", lien);
            String html = templateEngine.process("emails/generique", contexte);

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, false, "UTF-8");
            helper.setTo(email);
            helper.setSubject(sujet);
            helper.setText(html, true);
            mailSender.send(mail);
            logger.info("Email envoyé à {}", email);
        } catch (MessagingException e) {
            // Exception vérifiée transformée en non vérifiée : l'appelant
            // (NotificationService) l'attrape et marque la notification ECHEC.
            throw new RuntimeException("Échec de l'envoi de l'email à " + email, e);
        }
    }
}