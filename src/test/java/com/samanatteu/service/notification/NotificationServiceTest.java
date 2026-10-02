package com.samanatteu.service.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.entity.notification.Notification;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.notification.CanalNotification;
import com.samanatteu.enums.notification.StatutNotification;
import com.samanatteu.enums.notification.TypeNotification;
import com.samanatteu.repository.notification.NotificationRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Règles d'envoi : SMS toujours, email en plus s'il existe, une ligne par canal,
// un échec n'interrompt rien. Les envoyeurs sont des faux (interfaces mockées) :
// c'est justement l'intérêt des interfaces EnvoyeurSms / EnvoyeurEmail.
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EnvoyeurSms envoyeurSms;
    @Mock
    private EnvoyeurEmail envoyeurEmail;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private NotificationService notificationService;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private Utilisateur awa(String email) {
        Utilisateur awa = new Utilisateur();
        awa.setId(30L);
        awa.setTelephone("771234566");
        awa.setEmail(email);
        return awa;
    }

    // Les lignes enregistrées, dans l'ordre (SMS puis email).
    private List<Notification> lignesEnregistrees(int nombre) {
        ArgumentCaptor<Notification> capture = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(nombre)).save(capture.capture());
        return capture.getAllValues();
    }

    // ------------------------------------------------------------- canaux

    @Test
    void sansEmail_seulLeSmsPart() {
        notificationService.notifier(awa(null), TypeNotification.PAIEMENT_CONFIRME, "Paiement reçu");

        verify(envoyeurSms).envoyer("771234566", "Paiement reçu");
        verify(envoyeurEmail, never()).envoyer(anyString(), anyString(), anyString());
        Notification sms = lignesEnregistrees(1).get(0);
        assertEquals(CanalNotification.SMS, sms.getCanal());
        assertEquals(StatutNotification.ENVOYE, sms.getStatut());
        assertEquals(TypeNotification.PAIEMENT_CONFIRME, sms.getType());
        assertNotNull(sms.getDateEnvoi());
        assertNotNull(sms.getCreatedAt());
        assertNull(sms.getTitre());
    }

    // Un email vide ("") existe en base : il ne compte pas comme un email.
    @Test
    void emailVide_seulLeSmsPart() {
        notificationService.notifier(awa("  "), TypeNotification.BIENVENUE, "Bienvenue");

        verify(envoyeurEmail, never()).envoyer(anyString(), anyString(), anyString());
        lignesEnregistrees(1);
    }

    @Test
    void avecEmail_smsEtEmailPartent_uneLigneParCanal() {
        notificationService.notifier(awa("awa@mail.sn"), TypeNotification.RESULTAT_TIRAGE, "Vous avez gagné");

        verify(envoyeurSms).envoyer("771234566", "Vous avez gagné");
        verify(envoyeurEmail).envoyer("awa@mail.sn", "Résultat du tirage", "Vous avez gagné");
        List<Notification> lignes = lignesEnregistrees(2);
        assertEquals(CanalNotification.SMS, lignes.get(0).getCanal());
        assertEquals(CanalNotification.EMAIL, lignes.get(1).getCanal());
        // L'objet de l'email est gardé dans titre.
        assertEquals("Résultat du tirage", lignes.get(1).getTitre());
    }

    // ------------------------------------------------------------- échecs

    // Le fournisseur SMS plante : rien ne remonte (le paiement n'est pas annulé),
    // la ligne dit ECHEC, et l'email part quand même.
    @Test
    void smsEnEchec_neRemontePasEtLEmailPartQuandMeme() {
        doThrow(new RuntimeException("réseau coupé")).when(envoyeurSms).envoyer(anyString(), anyString());

        assertDoesNotThrow(() -> notificationService.notifier(awa("awa@mail.sn"),
                TypeNotification.PAIEMENT_CONFIRME, "Paiement reçu"));

        verify(envoyeurEmail).envoyer(any(), any(), any());
        List<Notification> lignes = lignesEnregistrees(2);
        assertEquals(StatutNotification.ECHEC, lignes.get(0).getStatut());
        assertNull(lignes.get(0).getDateEnvoi());
        assertEquals(StatutNotification.ENVOYE, lignes.get(1).getStatut());
    }

    @Test
    void emailEnEchec_leSmsResteEnvoye() {
        doThrow(new RuntimeException("SMTP indisponible")).when(envoyeurEmail)
                .envoyer(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> notificationService.notifier(awa("awa@mail.sn"),
                TypeNotification.BIENVENUE, "Bienvenue"));

        List<Notification> lignes = lignesEnregistrees(2);
        assertEquals(StatutNotification.ENVOYE, lignes.get(0).getStatut());
        assertEquals(StatutNotification.ECHEC, lignes.get(1).getStatut());
    }

    // ------------------------------------------------------------- lecture

    @Test
    void liste_neRenvoieQueLesNotificationsDuConnecte() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "771234566", null, List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"))));
        when(notificationRepository.findByDestinataireTelephoneOrderByCreatedAtDesc("771234566"))
                .thenReturn(List.of());

        notificationService.listNotification();

        verify(notificationRepository).findByDestinataireTelephoneOrderByCreatedAtDesc("771234566");
        verify(notificationRepository, never()).findAll();
    }
}
