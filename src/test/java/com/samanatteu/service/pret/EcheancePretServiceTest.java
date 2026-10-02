package com.samanatteu.service.pret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.ArgumentCaptor;

import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.notification.NotificationService;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.notification.TypeNotification;

// Tâche de minuit (échéances dépassées → EN_RETARD, prêt aussi) et
// lecture filtrée des échéances.
@ExtendWith(MockitoExtension.class)
class EcheancePretServiceTest {

    @Mock
    private EcheancePretRepository echeancePretRepository;
    @Mock
    private PretRepository pretRepository;
    // Les envois (SMS/email) sont vérifiés par NotificationServiceTest ; ici on
    // vérifie seulement que le service métier les DÉCLENCHE.
    @Mock
    private NotificationService notificationService;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private EcheancePretService echeancePretService;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void connecter(String telephone, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(telephone, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private EcheancePret echeanceDe(Pret pret) {
        EcheancePret e = new EcheancePret();
        e.setPret(pret);
        e.setStatut(StatutEcheancePret.EN_ATTENTE);
        return e;
    }

    // ------------------------------------------------------------- retards

    // On cherche les EN_ATTENTE strictement AVANT aujourd'hui (Before).
    @Test
    void marquerRetard_chercheLesEcheancesEnAttenteDepassees() {
        when(echeancePretRepository.findByStatutAndDateEcheanceBefore(StatutEcheancePret.EN_ATTENTE,
                LocalDate.now())).thenReturn(List.of());

        echeancePretService.marquerRetard();

        verify(echeancePretRepository).findByStatutAndDateEcheanceBefore(StatutEcheancePret.EN_ATTENTE,
                LocalDate.now());
        verify(pretRepository, never()).save(any());
    }

    @Test
    void marquerRetard_passeLEcheanceEtSonPretActifEnRetard() {
        Pret pret = new Pret();
        pret.setStatut(StatutPret.ACTIF);
        EcheancePret echeance = echeanceDe(pret);
        when(echeancePretRepository.findByStatutAndDateEcheanceBefore(StatutEcheancePret.EN_ATTENTE,
                LocalDate.now())).thenReturn(List.of(echeance));

        echeancePretService.marquerRetard();

        assertEquals(StatutEcheancePret.EN_RETARD, echeance.getStatut());
        verify(echeancePretRepository).save(echeance);
        assertEquals(StatutPret.EN_RETARD, pret.getStatut());
        verify(pretRepository).save(pret);
    }

    // Prêt déjà EN_RETARD (autre échéance) : on ne le réécrit pas.
    @Test
    void marquerRetard_pretDejaEnRetard_nEstPasReenregistre() {
        Pret pret = new Pret();
        pret.setStatut(StatutPret.EN_RETARD);
        EcheancePret echeance = echeanceDe(pret);
        when(echeancePretRepository.findByStatutAndDateEcheanceBefore(StatutEcheancePret.EN_ATTENTE,
                LocalDate.now())).thenReturn(List.of(echeance));

        echeancePretService.marquerRetard();

        assertEquals(StatutEcheancePret.EN_RETARD, echeance.getStatut());
        verify(pretRepository, never()).save(any());
    }

    // ------------------------------------------------------------- lecture

    @Test
    void lister_parUnGestionnaire_neLitQueLesEcheancesDeSesTontines() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(echeancePretRepository.findByPretTontineGestionnaireTelephone("770000101")).thenReturn(List.of());

        echeancePretService.listEcheancePret();

        verify(echeancePretRepository, never()).findAll();
        verify(echeancePretRepository, never()).findByPretMembreTelephone(anyString());
    }

    @Test
    void lister_parUnMembre_neLitQueSesPropresEcheances() {
        connecter("771234566", "ROLE_MEMBRE");
        when(echeancePretRepository.findByPretMembreTelephone("771234566")).thenReturn(List.of());

        echeancePretService.listEcheancePret();

        verify(echeancePretRepository, never()).findAll();
        verify(echeancePretRepository, never()).findByPretTontineGestionnaireTelephone(anyString());
    }

    // ------------------------------------------------------------ rappels J-3

    // Égalité sur la date (pas Before) : un seul rappel par échéance.
    @Test
    void rappel_chercheLesEcheancesEnAttenteQuiTombentDans3Jours() {
        when(echeancePretRepository.findByStatutAndDateEcheance(StatutEcheancePret.EN_ATTENTE,
                LocalDate.now().plusDays(3))).thenReturn(List.of());

        echeancePretService.rappelerEcheances();

        verify(echeancePretRepository).findByStatutAndDateEcheance(StatutEcheancePret.EN_ATTENTE,
                LocalDate.now().plusDays(3));
        verifyNoInteractions(notificationService);
    }

    // Reste dû (une partie a pu être payée en avance), date au format jj/mm/aaaa.
    @Test
    void rappel_annonceAuMembreLeResteDuEtLaDate() {
        Utilisateur membre = new Utilisateur();
        membre.setPrenom("Awa");
        membre.setNom("Diop");
        Tontine tontine = new Tontine();
        tontine.setNom("Natt des femmes");
        Pret pret = new Pret();
        pret.setMembre(membre);
        pret.setTontine(tontine);
        EcheancePret echeance = echeanceDe(pret);
        echeance.setMontantDu(new BigDecimal("11000"));
        echeance.setMontantPaye(new BigDecimal("1000"));
        LocalDate date = LocalDate.now().plusDays(3);
        echeance.setDateEcheance(date);
        when(echeancePretRepository.findByStatutAndDateEcheance(StatutEcheancePret.EN_ATTENTE, date))
                .thenReturn(List.of(echeance));

        echeancePretService.rappelerEcheances();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifier(eq(membre), eq(TypeNotification.RAPPEL_ECHEANCE),
                message.capture());
        assertTrue(message.getValue().startsWith("Natt des femmes : Awa Diop"));
        assertTrue(message.getValue().contains("échéance de 10000 F"));
        assertTrue(message.getValue().contains(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }
}
