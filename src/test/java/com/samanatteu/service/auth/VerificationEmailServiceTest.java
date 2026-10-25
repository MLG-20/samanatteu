package com.samanatteu.service.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.samanatteu.dto.auth.RenvoiVerificationDTO;
import com.samanatteu.entity.abonnement.Formule;
import com.samanatteu.entity.auth.VerificationEmail;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.auth.LienVerificationInvalideException;
import com.samanatteu.repository.auth.VerificationEmailRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.service.abonnement.AbonnementService;
import com.samanatteu.service.notification.EnvoyeurEmail;
import com.samanatteu.service.utilisateur.TelephoneService;

// Tests de la vérification d'email avec de faux repositories et un faux envoi
// d'email. L'empreinte SHA-256 est VRAIE : on vérifie que la base ne contient
// jamais le jeton du lien. Convention : la gestionnaire 40 (awa@example.sn,
// 771234566) a choisi la formule PRO, et son lien porte le jeton « jeton-valide ».
@ExtendWith(MockitoExtension.class)
class VerificationEmailServiceTest {

    private static final String FRONT = "http://front.test";

    @Mock
    private VerificationEmailRepository verificationEmailRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private AbonnementService abonnementService;
    @Mock
    private EnvoyeurEmail envoyeurEmail;

    private VerificationEmailService verificationEmailService;

    // Monté à la main, pas par @InjectMocks : l'adresse du front est une
    // @Value (une String), que Mockito ne sait pas fournir.
    @BeforeEach
    void monter() {
        verificationEmailService = new VerificationEmailService(verificationEmailRepository, utilisateurRepository,
                abonnementService, new TelephoneService("+221"), envoyeurEmail, FRONT);
    }

    // ------------------------------------------------------------------ aides

    private Utilisateur gestionnaire40(boolean emailVerifie) {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setId(40L);
        gestionnaire.setPrenom("Awa");
        gestionnaire.setEmail("awa@example.sn");
        gestionnaire.setTelephone("+221771234566");
        gestionnaire.setRole(RoleUtilisateur.GESTIONNAIRE);
        gestionnaire.setEmailVerifie(emailVerifie);
        return gestionnaire;
    }

    private Formule pro() {
        Formule formule = new Formule();
        formule.setId(2L);
        formule.setNom("PRO");
        formule.setPrixMensuel(new BigDecimal("25000"));
        formule.setActive(true);
        return formule;
    }

    // Un lien encore utilisable : non cliqué, envoyé il y a 5 minutes, expire dans 23 h.
    private VerificationEmail lien(Utilisateur gestionnaire) {
        VerificationEmail verification = new VerificationEmail();
        verification.setId(7L);
        verification.setUtilisateur(gestionnaire);
        verification.setFormule(pro());
        verification.setJetonHache(VerificationEmailService.empreinte("jeton-valide"));
        verification.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        verification.setExpireAt(LocalDateTime.now().plusHours(23));
        return verification;
    }

    // Le lien « jeton-valide » est en base.
    private void enBase(VerificationEmail verification) {
        when(verificationEmailRepository.findByJetonHache(VerificationEmailService.empreinte("jeton-valide")))
                .thenReturn(Optional.of(verification));
    }

    private RenvoiVerificationDTO renvoiParEmail(String email) {
        RenvoiVerificationDTO dto = new RenvoiVerificationDTO();
        dto.setEmail(email);
        return dto;
    }

    private RenvoiVerificationDTO renvoiParTelephone(String telephone) {
        RenvoiVerificationDTO dto = new RenvoiVerificationDTO();
        dto.setTelephone(telephone);
        return dto;
    }

    // ------------------------------------------------------------- envoyerLien

    // Règle : l'email contient le lien vers la page du front ; la base ne garde
    // que l'EMPREINTE du jeton, avec la formule choisie et 24 h de validité.
    @Test
    void envoyerLien_enregistreLEmpreinteEtEnvoieLeJetonParEmail() {
        Utilisateur gestionnaire = gestionnaire40(false);

        verificationEmailService.envoyerLien(gestionnaire, pro());

        ArgumentCaptor<String> lienEnvoye = ArgumentCaptor.forClass(String.class);
        verify(envoyeurEmail).envoyerAvecLien(eq("awa@example.sn"), anyString(), anyString(), anyString(),
                lienEnvoye.capture());
        assertTrue(lienEnvoye.getValue().startsWith(FRONT + "/verifier-email/"));
        String jeton = lienEnvoye.getValue().substring((FRONT + "/verifier-email/").length());

        ArgumentCaptor<VerificationEmail> enregistree = ArgumentCaptor.forClass(VerificationEmail.class);
        verify(verificationEmailRepository).save(enregistree.capture());
        VerificationEmail verification = enregistree.getValue();
        assertNotEquals(jeton, verification.getJetonHache());
        assertEquals(VerificationEmailService.empreinte(jeton), verification.getJetonHache());
        assertEquals("PRO", verification.getFormule().getNom());
        assertFalse(verification.isUtilise());
        assertTrue(verification.getExpireAt().isAfter(LocalDateTime.now().plusHours(23)));
        assertTrue(verification.getExpireAt().isBefore(LocalDateTime.now().plusHours(25)));
    }

    // Règle : un email raté n'annule pas l'inscription (le lien reste en base,
    // la gestionnaire pourra en redemander un).
    @Test
    void envoyerLien_emailEnEchec_neLevePasDErreur() {
        doThrow(new RuntimeException("SMTP injoignable")).when(envoyeurEmail)
                .envoyerAvecLien(anyString(), anyString(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> verificationEmailService.envoyerLien(gestionnaire40(false), pro()));

        verify(verificationEmailRepository).save(any(VerificationEmail.class));
    }

    // ---------------------------------------------------------------- verifier

    // Règle : le clic vérifie l'email, consomme le lien, et démarre l'essai sur
    // la formule du lien.
    @Test
    void verifier_lienValide_verifieLEmailEtDemarreLEssai() {
        Utilisateur gestionnaire = gestionnaire40(false);
        VerificationEmail verification = lien(gestionnaire);
        enBase(verification);

        verificationEmailService.verifier("jeton-valide");

        assertTrue(gestionnaire.isEmailVerifie());
        verify(utilisateurRepository).save(gestionnaire);
        assertTrue(verification.isUtilise());
        verify(verificationEmailRepository).save(verification);
        verify(abonnementService).demarrerEssai(gestionnaire, verification.getFormule());
    }

    @Test
    void verifier_jetonInconnu_estRefuse() {
        when(verificationEmailRepository.findByJetonHache(anyString())).thenReturn(Optional.empty());

        assertThrows(LienVerificationInvalideException.class,
                () -> verificationEmailService.verifier("jeton-invente"));

        verifyNoInteractions(abonnementService);
    }

    @Test
    void verifier_lienExpire_estRefuse() {
        Utilisateur gestionnaire = gestionnaire40(false);
        VerificationEmail verification = lien(gestionnaire);
        verification.setExpireAt(LocalDateTime.now().minusMinutes(1));
        enBase(verification);

        assertThrows(LienVerificationInvalideException.class,
                () -> verificationEmailService.verifier("jeton-valide"));

        assertFalse(gestionnaire.isEmailVerifie());
        verifyNoInteractions(abonnementService);
    }

    // Règle : usage unique (un second clic ne refait rien).
    @Test
    void verifier_lienDejaUtilise_estRefuse() {
        VerificationEmail verification = lien(gestionnaire40(false));
        verification.setUtilise(true);
        enBase(verification);

        assertThrows(LienVerificationInvalideException.class,
                () -> verificationEmailService.verifier("jeton-valide"));

        verifyNoInteractions(abonnementService);
    }

    // Règle : un ancien lien encore valable ne relance pas 30 jours d'essai
    // une fois le compte vérifié par un autre lien.
    @Test
    void verifier_compteDejaVerifie_neRedemarrePasLEssai() {
        enBase(lien(gestionnaire40(true)));

        assertThrows(LienVerificationInvalideException.class,
                () -> verificationEmailService.verifier("jeton-valide"));

        verifyNoInteractions(abonnementService);
    }

    // ---------------------------------------------------------------- renvoyer

    // Règle : un compte non vérifié reçoit un nouveau lien, sur la formule
    // choisie à l'inscription.
    @Test
    void renvoyer_compteNonVerifie_envoieUnNouveauLienSurLaMemeFormule() {
        Utilisateur gestionnaire = gestionnaire40(false);
        when(utilisateurRepository.findByEmail("awa@example.sn")).thenReturn(Optional.of(gestionnaire));
        when(verificationEmailRepository.findFirstByUtilisateurIdOrderByCreatedAtDesc(40L))
                .thenReturn(Optional.of(lien(gestionnaire)));

        verificationEmailService.renvoyer(renvoiParEmail("awa@example.sn"));

        ArgumentCaptor<VerificationEmail> enregistree = ArgumentCaptor.forClass(VerificationEmail.class);
        verify(verificationEmailRepository).save(enregistree.capture());
        assertEquals("PRO", enregistree.getValue().getFormule().getNom());
        verify(envoyeurEmail).envoyerAvecLien(eq("awa@example.sn"), anyString(), anyString(), anyString(),
                anyString());
    }

    // Règle : même identifiant qu'à la connexion ; le numéro saisi est normalisé.
    @Test
    void renvoyer_parTelephone_trouveLeCompteAvecLeNumeroNormalise() {
        Utilisateur gestionnaire = gestionnaire40(false);
        when(utilisateurRepository.findByTelephone("+221771234566")).thenReturn(Optional.of(gestionnaire));
        when(verificationEmailRepository.findFirstByUtilisateurIdOrderByCreatedAtDesc(40L))
                .thenReturn(Optional.of(lien(gestionnaire)));

        verificationEmailService.renvoyer(renvoiParTelephone("77 123 45 66"));

        verify(envoyeurEmail).envoyerAvecLien(eq("awa@example.sn"), anyString(), anyString(), anyString(),
                anyString());
    }

    // Les cas suivants ne font RIEN, sans erreur : la route répond toujours 204
    // et ne révèle pas si le compte existe.
    @Test
    void renvoyer_compteInconnu_neFaitRien() {
        when(utilisateurRepository.findByEmail("inconnu@example.sn")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> verificationEmailService.renvoyer(renvoiParEmail("inconnu@example.sn")));

        verifyNoInteractions(envoyeurEmail);
        verify(verificationEmailRepository, never()).save(any());
    }

    @Test
    void renvoyer_numeroMalForme_neFaitRien() {
        assertDoesNotThrow(() -> verificationEmailService.renvoyer(renvoiParTelephone("pas-un-numero")));

        verifyNoInteractions(envoyeurEmail);
        verify(verificationEmailRepository, never()).save(any());
    }

    @Test
    void renvoyer_compteDejaVerifie_neFaitRien() {
        when(utilisateurRepository.findByEmail("awa@example.sn")).thenReturn(Optional.of(gestionnaire40(true)));

        verificationEmailService.renvoyer(renvoiParEmail("awa@example.sn"));

        verifyNoInteractions(envoyeurEmail);
        verify(verificationEmailRepository, never()).save(any());
    }

    // Règle : un renvoi par minute au plus (« Renvoyer l'email » ne doit pas
    // servir à inonder une boîte mail).
    @Test
    void renvoyer_demandeTropRapprochee_neFaitRien() {
        Utilisateur gestionnaire = gestionnaire40(false);
        VerificationEmail recent = lien(gestionnaire);
        recent.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        when(utilisateurRepository.findByEmail("awa@example.sn")).thenReturn(Optional.of(gestionnaire));
        when(verificationEmailRepository.findFirstByUtilisateurIdOrderByCreatedAtDesc(40L))
                .thenReturn(Optional.of(recent));

        verificationEmailService.renvoyer(renvoiParEmail("awa@example.sn"));

        verifyNoInteractions(envoyeurEmail);
        verify(verificationEmailRepository, never()).save(any());
    }
}
