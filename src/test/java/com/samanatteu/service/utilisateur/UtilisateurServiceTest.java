package com.samanatteu.service.utilisateur;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.samanatteu.dto.utilisateur.CreationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.ModificationUtilisateurDTO;
import com.samanatteu.dto.utilisateur.UtilisateurDTO;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.RoleUtilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.EmailDejaUtiliseException;
import com.samanatteu.repository.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    // Un vrai objet (pas un faux) : il lit le SecurityContextHolder rempli par connecterComme...().
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private UtilisateurService utilisateurService;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void connecterCommeMembre(String telephone) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(telephone, null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"))));
    }

    // Une inscription est faite par quelqu'un qui n'a pas encore de compte : Spring Security
    // le représente par un jeton "anonyme", sans rôle ADMIN/GESTIONNAIRE/MEMBRE.
    private void connecterCommeAnonyme() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("cle", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }

    private CreationUtilisateurDTO inscription(RoleUtilisateur role) {
        CreationUtilisateurDTO dto = new CreationUtilisateurDTO();
        dto.setNom("Fall");
        dto.setPrenom("Moussa");
        dto.setTelephone("775550000");
        dto.setMotDePasse("motdepasse123");
        dto.setRole(role);
        return dto;
    }

    @Test
    void createUtilisateur_accepteUnMembreAnonyme() {
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(appel -> appel.getArgument(0));
        connecterCommeAnonyme();

        UtilisateurDTO resultat = utilisateurService.createUtilisateur(inscription(RoleUtilisateur.MEMBRE));

        assertEquals(RoleUtilisateur.MEMBRE, resultat.getRole());
    }

    @Test
    void createUtilisateur_refuseUnAdminDemandeParUnAnonyme() {
        connecterCommeAnonyme();

        assertThrows(AccesRefuseException.class,
                () -> utilisateurService.createUtilisateur(inscription(RoleUtilisateur.ADMIN)));

        verify(utilisateurRepository, never()).save(any());
    }

    private Utilisateur membreEnBase(String telephone, String email) {
        Utilisateur membre = new Utilisateur();
        membre.setId(17L);
        membre.setNom("Diop");
        membre.setPrenom("Awa");
        membre.setTelephone(telephone);
        membre.setEmail(email);
        membre.setRole(RoleUtilisateur.MEMBRE);
        membre.setActif(true);
        return membre;
    }

    private ModificationUtilisateurDTO modifications(String nom, String prenom, String email) {
        ModificationUtilisateurDTO dto = new ModificationUtilisateurDTO();
        dto.setNom(nom);
        dto.setPrenom(prenom);
        dto.setEmail(email);
        return dto;
    }

    // Le test de la faille "mass assignment" : la modification de profil ne touche
    // qu'à nom, prénom et email. Le rôle et le statut actif restent ceux de la base.
    @Test
    void updateUtilisateur_neChangeNiLeRoleNiActif() {
        Utilisateur membre = membreEnBase("771234566", null);
        when(utilisateurRepository.findById(17L)).thenReturn(Optional.of(membre));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(appel -> appel.getArgument(0));
        connecterCommeMembre("771234566");

        UtilisateurDTO resultat = utilisateurService
                .updateUtilisateur(17L, modifications("Ndiaye", "Awa", null)).orElseThrow();

        assertEquals("Ndiaye", resultat.getNom());
        assertEquals(RoleUtilisateur.MEMBRE, resultat.getRole());
        assertTrue(resultat.getActif());
    }

    @Test
    void updateUtilisateur_refuseSiPasSonProfil() {
        when(utilisateurRepository.findById(17L)).thenReturn(Optional.of(membreEnBase("771234566", null)));
        connecterCommeMembre("770000000");

        assertThrows(AccesRefuseException.class,
                () -> utilisateurService.updateUtilisateur(17L, modifications("Ndiaye", "Awa", null)));

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void updateUtilisateur_refuseUnEmailDejaPrisParUnAutre() {
        when(utilisateurRepository.findById(17L))
                .thenReturn(Optional.of(membreEnBase("771234566", "awa@mail.sn")));
        when(utilisateurRepository.existsByEmail("moussa@mail.sn")).thenReturn(true);
        connecterCommeMembre("771234566");

        assertThrows(EmailDejaUtiliseException.class,
                () -> utilisateurService.updateUtilisateur(17L, modifications("Diop", "Awa", "moussa@mail.sn")));

        verify(utilisateurRepository, never()).save(any());
    }

    // Renvoyer son propre email n'est pas un conflit, et ne doit même pas
    // interroger la base (le "&&" s'arrête avant existsByEmail).
    @Test
    void updateUtilisateur_accepteSonPropreEmail() {
        when(utilisateurRepository.findById(17L))
                .thenReturn(Optional.of(membreEnBase("771234566", "awa@mail.sn")));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(appel -> appel.getArgument(0));
        connecterCommeMembre("771234566");

        assertDoesNotThrow(
                () -> utilisateurService.updateUtilisateur(17L, modifications("Diop", "Awa", "awa@mail.sn")));

        verify(utilisateurRepository, never()).existsByEmail(anyString());
    }

    @Test
    void updateUtilisateur_enregistreUnEmailVideCommeNull() {
        when(utilisateurRepository.findById(17L))
                .thenReturn(Optional.of(membreEnBase("771234566", "awa@mail.sn")));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(appel -> appel.getArgument(0));
        connecterCommeMembre("771234566");

        UtilisateurDTO resultat = utilisateurService
                .updateUtilisateur(17L, modifications("Diop", "Awa", "   ")).orElseThrow();

        assertNull(resultat.getEmail());
        verify(utilisateurRepository, never()).existsByEmail(anyString());
    }
}
