package com.samanatteu.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.samanatteu.dto.auth.ChangementMotDePasseDTO;
import com.samanatteu.dto.auth.RefreshRequestDTO;
import com.samanatteu.dto.auth.TokenDTO;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.auth.AncienMotDePasseIncorrectException;
import com.samanatteu.exception.auth.RefreshTokenInvalideException;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.security.UtilisateurConnecte;

// Tests des règles d'AuthService avec un faux repository (Mockito).
// Le hachage, lui, est VRAI (BCrypt) : c'est justement ce qu'on veut vérifier
// (le mot de passe enregistré n'est jamais le texte tapé). Les tokens aussi sont de
// VRAIS JWT signés : on vérifie le numéro de version écrit dedans.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Spy
    private JwtUtil jwtUtil = vraiJwtUtil();
    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ aides

    // Hors de Spring, personne n'injecte les @Value ni n'appelle le @PostConstruct :
    // on le fait à la main (même montage que JwtAuthFilterTest).
    private static JwtUtil vraiJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "une-cle-de-test-d-au-moins-trente-deux-octets");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 900_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 604_800_000L);
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");
        return jwtUtil;
    }

    private RefreshRequestDTO demandeRefresh(String refreshToken) {
        RefreshRequestDTO dto = new RefreshRequestDTO();
        dto.setRefreshToken(refreshToken);
        return dto;
    }

    private void connecter(String telephone) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(telephone, null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"))));
    }

    // Le compte 771234566, dont le mot de passe actuel est « motdepasse123 » (haché).
    private Utilisateur compte(String motDePasseEnClair) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(30L);
        utilisateur.setTelephone("771234566");
        utilisateur.setRole(RoleUtilisateur.MEMBRE);
        utilisateur.setMotDePasse(motDePasseEnClair == null ? null : passwordEncoder.encode(motDePasseEnClair));
        return utilisateur;
    }

    private ChangementMotDePasseDTO demande(String ancien, String nouveau) {
        ChangementMotDePasseDTO dto = new ChangementMotDePasseDTO();
        dto.setAncienMotDePasse(ancien);
        dto.setNouveauMotDePasse(nouveau);
        return dto;
    }

    // ------------------------------------------------------ changerMotDePasse

    // Le compte modifié est celui du token ; le nouveau mot de passe est enregistré
    // haché (jamais en clair) et c'est bien lui qui ouvre le compte ensuite.
    @Test
    void changerMotDePasse_ancienCorrect_enregistreLeNouveauHache() {
        connecter("771234566");
        Utilisateur utilisateur = compte("motdepasse123");
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));

        authService.changerMotDePasse(demande("motdepasse123", "nouveaupasse456"));

        verify(utilisateurRepository).save(utilisateur);
        assertNotEquals("nouveaupasse456", utilisateur.getMotDePasse());
        assertTrue(passwordEncoder.matches("nouveaupasse456", utilisateur.getMotDePasse()));
        assertFalse(passwordEncoder.matches("motdepasse123", utilisateur.getMotDePasse()));
        // Les sessions ouvertes sont fermées en même temps (numéro de version + 1).
        assertEquals(1, utilisateur.getVersionSessions());
    }

    // Sans la preuve de l'ancien mot de passe, rien ne change et rien n'est enregistré.
    @Test
    void changerMotDePasse_ancienIncorrect_estRefuseSansRienChanger() {
        connecter("771234566");
        Utilisateur utilisateur = compte("motdepasse123");
        String hacheAvant = utilisateur.getMotDePasse();
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));

        assertThrows(AncienMotDePasseIncorrectException.class,
                () -> authService.changerMotDePasse(demande("pas-le-bon", "nouveaupasse456")));

        assertEquals(hacheAvant, utilisateur.getMotDePasse());
        assertEquals(0, utilisateur.getVersionSessions());
        verify(utilisateurRepository, never()).save(any());
    }

    // Compte créé par l'import : pas de mot de passe du tout. Il ne peut pas en
    // « changer » (aucun ancien n'est le bon) : il passera par « mot de passe oublié ».
    @Test
    void changerMotDePasse_compteSansMotDePasse_estRefuse() {
        connecter("771234566");
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(compte(null)));

        assertThrows(AncienMotDePasseIncorrectException.class,
                () -> authService.changerMotDePasse(demande("nimportequoi", "nouveaupasse456")));

        verify(utilisateurRepository, never()).save(any());
    }

    // Token encore valide mais compte supprimé entre-temps.
    @Test
    void changerMotDePasse_compteDisparu_estRefuse() {
        connecter("771234566");
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.empty());

        assertThrows(AccesRefuseException.class,
                () -> authService.changerMotDePasse(demande("motdepasse123", "nouveaupasse456")));

        verify(utilisateurRepository, never()).save(any());
    }

    // ------------------------------------------------- déconnexion et refresh

    @Test
    void refresh_tokenALaVersionDuCompte_donneUneNouvellePaire() {
        Utilisateur utilisateur = compte("motdepasse123");
        String refreshToken = jwtUtil.generateRefreshToken(utilisateur);
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));

        TokenDTO paire = authService.refresh(demandeRefresh(refreshToken));

        assertEquals("access", jwtUtil.extractType(paire.getAccessToken()));
        assertEquals("refresh", jwtUtil.extractType(paire.getRefreshToken()));
        assertEquals(0, jwtUtil.extractVersion(paire.getRefreshToken()));
    }

    // Un JWT ne s'annule pas : c'est le numéro de version du compte qui change, et
    // le token fabriqué avant ne lui correspond plus.
    @Test
    void deconnecter_perimeLesRefreshTokensDejaFabriques() {
        connecter("771234566");
        Utilisateur utilisateur = compte("motdepasse123");
        String refreshToken = jwtUtil.generateRefreshToken(utilisateur);
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));

        authService.deconnecter();

        assertEquals(1, utilisateur.getVersionSessions());
        verify(utilisateurRepository).save(utilisateur);
        assertThrows(RefreshTokenInvalideException.class,
                () -> authService.refresh(demandeRefresh(refreshToken)));
    }

    // Un token fabriqué APRÈS la déconnexion porte le nouveau numéro : il est accepté.
    @Test
    void refresh_tokenFabriqueApresLaDeconnexion_estAccepte() {
        connecter("771234566");
        Utilisateur utilisateur = compte("motdepasse123");
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));
        authService.deconnecter();

        String nouveau = jwtUtil.generateRefreshToken(utilisateur);

        assertEquals(1, jwtUtil.extractVersion(authService.refresh(demandeRefresh(nouveau)).getRefreshToken()));
    }

    // Token fabriqué avant l'existence du numéro de version : pas de claim, refusé
    // (et pas de NullPointerException sur la comparaison).
    @Test
    void refresh_ancienTokenSansVersion_estRefuse() {
        Utilisateur utilisateur = compte("motdepasse123");
        String refreshToken = jwtUtil.generateRefreshToken(utilisateur);
        doReturn(null).when(jwtUtil).extractVersion(refreshToken);
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(utilisateur));

        assertThrows(RefreshTokenInvalideException.class,
                () -> authService.refresh(demandeRefresh(refreshToken)));
    }

    // Un access token ne peut pas servir à en obtenir d'autres.
    @Test
    void refresh_avecUnAccessToken_estRefuse() {
        Utilisateur utilisateur = compte("motdepasse123");
        String accessToken = jwtUtil.generateToken(utilisateur);

        assertThrows(RefreshTokenInvalideException.class,
                () -> authService.refresh(demandeRefresh(accessToken)));
    }

    @Test
    void refresh_tokenIllisible_estRefuse() {
        assertThrows(RefreshTokenInvalideException.class,
                () -> authService.refresh(demandeRefresh("pas-un-jwt")));
    }
}
