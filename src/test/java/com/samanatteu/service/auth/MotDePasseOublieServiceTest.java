package com.samanatteu.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.samanatteu.dto.auth.MotDePasseOublieDTO;
import com.samanatteu.dto.auth.ReinitialisationMotDePasseDTO;
import com.samanatteu.entity.auth.CodeReinitialisation;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.exception.auth.CodeInvalideException;
import com.samanatteu.repository.auth.CodeReinitialisationRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.service.notification.EnvoyeurSms;

// Tests de « mot de passe oublié » avec de faux repositories et un faux envoi de SMS.
// Le hachage est VRAI (BCrypt) : on vérifie justement que ni le code ni le mot de
// passe ne sont enregistrés en clair. Convention : le compte 30 a le téléphone
// 771234566 et le code « 123456 ».
@ExtendWith(MockitoExtension.class)
class MotDePasseOublieServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private CodeReinitialisationRepository codeReinitialisationRepository;
    @Mock
    private EnvoyeurSms envoyeurSms;
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private MotDePasseOublieService motDePasseOublieService;

    // ------------------------------------------------------------------ aides

    // Compte créé par l'import : pas encore de mot de passe.
    private Utilisateur compte30() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(30L);
        utilisateur.setTelephone("771234566");
        return utilisateur;
    }

    // Un code « 123456 » encore utilisable : non utilisé, 0 essai, expire dans 5 minutes.
    private CodeReinitialisation code123456(Utilisateur utilisateur) {
        CodeReinitialisation code = new CodeReinitialisation();
        code.setId(7L);
        code.setUtilisateur(utilisateur);
        code.setCodeHache(passwordEncoder.encode("123456"));
        code.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        code.setExpireAt(LocalDateTime.now().plusMinutes(5));
        return code;
    }

    private MotDePasseOublieDTO demande(String telephone) {
        MotDePasseOublieDTO dto = new MotDePasseOublieDTO();
        dto.setTelephone(telephone);
        return dto;
    }

    private ReinitialisationMotDePasseDTO reinitialisation(String telephone, String code) {
        ReinitialisationMotDePasseDTO dto = new ReinitialisationMotDePasseDTO();
        dto.setTelephone(telephone);
        dto.setCode(code);
        dto.setNouveauMotDePasse("nouveaupasse456");
        return dto;
    }

    // Le compte 30 existe et son dernier code est celui fourni.
    private void preparer(Utilisateur utilisateur, CodeReinitialisation code) {
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));
        when(codeReinitialisationRepository.findFirstByUtilisateurIdOrderByCreatedAtDesc(30L))
                .thenReturn(Optional.of(code));
    }

    // ------------------------------------------------------------ demanderCode

    // Numéro inconnu : aucune erreur (la réponse ne révèle pas qui a un compte),
    // mais rien n'est créé ni envoyé.
    @Test
    void demanderCode_numeroInconnu_neFaitRienSansErreur() {
        when(utilisateurRepository.findByTelephone("779999999")).thenReturn(Optional.empty());

        motDePasseOublieService.demanderCode(demande("779999999"));

        verifyNoInteractions(codeReinitialisationRepository, envoyeurSms);
    }

    // Le code part en clair dans le SMS (6 chiffres) mais n'est enregistré que haché,
    // valable 10 minutes, jamais encore essayé ni utilisé.
    @Test
    void demanderCode_numeroConnu_envoieLeCodeEtNEnregistreQueSonHachage() {
        Utilisateur utilisateur = compte30();
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));

        motDePasseOublieService.demanderCode(demande("771234566"));

        ArgumentCaptor<String> sms = ArgumentCaptor.forClass(String.class);
        verify(envoyeurSms).envoyer(eq("771234566"), sms.capture());
        Matcher chiffres = Pattern.compile("votre code est (\\d{6})\\.").matcher(sms.getValue());
        assertTrue(chiffres.find(), "le SMS doit contenir un code à 6 chiffres : " + sms.getValue());
        String codeEnvoye = chiffres.group(1);

        ArgumentCaptor<CodeReinitialisation> enregistre = ArgumentCaptor.forClass(CodeReinitialisation.class);
        verify(codeReinitialisationRepository).save(enregistre.capture());
        CodeReinitialisation code = enregistre.getValue();
        assertEquals(utilisateur, code.getUtilisateur());
        assertNotEquals(codeEnvoye, code.getCodeHache());
        assertTrue(passwordEncoder.matches(codeEnvoye, code.getCodeHache()));
        assertTrue(code.getExpireAt().isAfter(LocalDateTime.now().plusMinutes(9)));
        assertTrue(code.getExpireAt().isBefore(LocalDateTime.now().plusMinutes(11)));
        assertEquals(0, code.getTentatives());
        assertFalse(code.isUtilise());
    }

    // ------------------------------------------------------------ reinitialiser

    // Cas d'un membre importé : il choisit ici son PREMIER mot de passe. Le mot de
    // passe est enregistré haché, et le code ne peut plus resservir.
    @Test
    void reinitialiser_bonCode_changeLeMotDePasseEtConsommeLeCode() {
        Utilisateur utilisateur = compte30();
        CodeReinitialisation code = code123456(utilisateur);
        preparer(utilisateur, code);

        motDePasseOublieService.reinitialiser(reinitialisation("771234566", "123456"));

        assertNotEquals("nouveaupasse456", utilisateur.getMotDePasse());
        assertTrue(passwordEncoder.matches("nouveaupasse456", utilisateur.getMotDePasse()));
        verify(utilisateurRepository).save(utilisateur);
        assertTrue(code.isUtilise());
        verify(codeReinitialisationRepository).save(code);
        // Nouveau mot de passe = sessions ouvertes fermées (numéro de version + 1).
        assertEquals(1, utilisateur.getVersionSessions());
    }

    // L'essai raté est compté ET enregistré : c'est ce compteur qui bloque celui
    // qui essaierait les codes un par un.
    @Test
    void reinitialiser_mauvaisCode_compteLEssaiSansChangerLeMotDePasse() {
        Utilisateur utilisateur = compte30();
        CodeReinitialisation code = code123456(utilisateur);
        preparer(utilisateur, code);

        assertThrows(CodeInvalideException.class,
                () -> motDePasseOublieService.reinitialiser(reinitialisation("771234566", "000000")));

        assertEquals(1, code.getTentatives());
        verify(codeReinitialisationRepository).save(code);
        assertFalse(code.isUtilise());
        assertNull(utilisateur.getMotDePasse());
        assertEquals(0, utilisateur.getVersionSessions());
        verify(utilisateurRepository, never()).save(any());
    }

    // Même avec le BON code, un code déjà utilisé, expiré ou trop essayé est refusé.
    @ParameterizedTest
    @ValueSource(strings = { "utilise", "expire", "tropDEssais" })
    void reinitialiser_codePlusUtilisable_estRefuseMemeAvecLeBonCode(String cas) {
        Utilisateur utilisateur = compte30();
        CodeReinitialisation code = code123456(utilisateur);
        switch (cas) {
            case "utilise" -> code.setUtilise(true);
            case "expire" -> code.setExpireAt(LocalDateTime.now().minusSeconds(1));
            default -> code.setTentatives(5);
        }
        preparer(utilisateur, code);

        assertThrows(CodeInvalideException.class,
                () -> motDePasseOublieService.reinitialiser(reinitialisation("771234566", "123456")));

        assertNull(utilisateur.getMotDePasse());
        verify(utilisateurRepository, never()).save(any());
        verify(codeReinitialisationRepository, never()).save(any());
    }

    // Quatre essais ratés laissent encore une chance : la limite est à 5.
    @Test
    void reinitialiser_quatreEssaisRates_leBonCodePasseEncore() {
        Utilisateur utilisateur = compte30();
        CodeReinitialisation code = code123456(utilisateur);
        code.setTentatives(4);
        preparer(utilisateur, code);

        motDePasseOublieService.reinitialiser(reinitialisation("771234566", "123456"));

        assertTrue(code.isUtilise());
    }

    // Même refus, même message que pour un code faux : on ne révèle pas que le
    // numéro n'a pas de compte.
    @Test
    void reinitialiser_numeroInconnu_donneLeMemeRefus() {
        when(utilisateurRepository.findByTelephone("779999999")).thenReturn(Optional.empty());

        CodeInvalideException refus = assertThrows(CodeInvalideException.class,
                () -> motDePasseOublieService.reinitialiser(reinitialisation("779999999", "123456")));

        assertEquals("Code invalide ou expiré. Demandez un nouveau code.", refus.getMessage());
        verifyNoInteractions(codeReinitialisationRepository);
    }

    // Compte existant mais aucun code demandé : deviner un code ne sert à rien.
    @Test
    void reinitialiser_aucunCodeDemande_estRefuse() {
        Utilisateur utilisateur = compte30();
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));
        when(codeReinitialisationRepository.findFirstByUtilisateurIdOrderByCreatedAtDesc(30L))
                .thenReturn(Optional.empty());

        assertThrows(CodeInvalideException.class,
                () -> motDePasseOublieService.reinitialiser(reinitialisation("771234566", "123456")));

        verify(utilisateurRepository, never()).save(any());
    }
}
