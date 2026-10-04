package com.samanatteu.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.samanatteu.dto.auth.ChangementMotDePasseDTO;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.auth.AncienMotDePasseIncorrectException;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.JwtUtil;
import com.samanatteu.security.UtilisateurConnecte;

// Tests des règles d'AuthService avec un faux repository (Mockito).
// Le hachage, lui, est VRAI (BCrypt) : c'est justement ce qu'on veut vérifier
// (le mot de passe enregistré n'est jamais le texte tapé).
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private JwtUtil jwtUtil;
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
}
