package com.samanatteu.service.onboarding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.dto.onboarding.DemandeInvitationDTO;
import com.samanatteu.dto.onboarding.InvitationDTO;
import com.samanatteu.dto.onboarding.LienInvitationDTO;
import com.samanatteu.entity.onboarding.Invitation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.onboarding.StatutInvitation;
import com.samanatteu.enums.onboarding.TypeInvitation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.onboarding.InvitationAnnuleeException;
import com.samanatteu.exception.onboarding.InvitationDejaUtiliseeException;
import com.samanatteu.exception.onboarding.InvitationExpireeException;
import com.samanatteu.exception.onboarding.InvitationIntrouvableException;
import com.samanatteu.exception.tontine.InscriptionsFermeesException;
import com.samanatteu.exception.tontine.ParticipationDejaExistanteException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.repository.onboarding.InvitationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.tontine.ParticipationService;

// Tests des règles d'InvitationService avec de faux repositories (Mockito).
// Convention : la tontine 6 appartient au gestionnaire 770000101 ; 770000102 est un
// autre gestionnaire ; le membre 30 a le téléphone 771234566.
@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    // Les règles d'inscription (doublon, parts, ordre, SMS de bienvenue) sont vérifiées
    // par ParticipationServiceTest ; ici on vérifie seulement que rejoindre APPELLE
    // l'inscription avec la bonne tontine, le bon membre et le bon nombre de parts.
    @Mock
    private ParticipationService participationService;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private InvitationService invitationService;

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ aides

    private void connecter(String telephone, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(telephone, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private Tontine tontine6(StatutTontine statut) {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setId(18L);
        gestionnaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setNom("Natt des femmes");
        tontine.setGestionnaire(gestionnaire);
        tontine.setStatut(statut);
        return tontine;
    }

    private Utilisateur membre30() {
        Utilisateur membre = new Utilisateur();
        membre.setId(30L);
        membre.setTelephone("771234566");
        return membre;
    }

    // Une invitation valide (EN_ATTENTE, expire dans 3 jours) sur une tontine EN_ATTENTE.
    private Invitation invitation(TypeInvitation type) {
        Invitation invitation = new Invitation();
        invitation.setId(50L);
        invitation.setTontine(tontine6(StatutTontine.EN_ATTENTE));
        invitation.setType(type);
        invitation.setToken("jeton");
        invitation.setStatut(StatutInvitation.EN_ATTENTE);
        invitation.setCreatedAt(LocalDateTime.now().minusDays(4));
        invitation.setExpireAt(LocalDateTime.now().plusDays(3));
        return invitation;
    }

    // save() renvoie l'objet reçu, comme le ferait la base.
    private void saveRenvoieLObjet() {
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    private DemandeInvitationDTO demande(Integer parts) {
        DemandeInvitationDTO demande = new DemandeInvitationDTO();
        demande.setTelephone("771234566");
        demande.setPrenom("Awa");
        demande.setNom("Diop");
        demande.setNombreParts(parts);
        return demande;
    }

    // ------------------------------------------------------- lien de groupe

    @Test
    void lienDeGroupe_tontineInexistante_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.empty());

        assertThrows(TontineIntrouvableException.class, () -> invitationService.genererLienGroupe(6L));
    }

    @Test
    void lienDeGroupe_parUnAutreGestionnaire_estRefuseSansRienEcrire() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));

        assertThrows(AccesRefuseException.class, () -> invitationService.genererLienGroupe(6L));
        verify(invitationRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatutTontine.class, names = { "ACTIVE", "SUSPENDUE", "TERMINEE" })
    void lienDeGroupe_tontineDemarree_inscriptionsFermees(StatutTontine statut) {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(statut)));

        assertThrows(InscriptionsFermeesException.class, () -> invitationService.genererLienGroupe(6L));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void lienDeGroupe_creeUnLienFixeParLeServeur() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));
        when(invitationRepository.findByTontineIdAndTypeAndStatut(6L, TypeInvitation.GROUPE,
                StatutInvitation.EN_ATTENTE)).thenReturn(List.of());
        saveRenvoieLObjet();

        InvitationDTO dto = invitationService.genererLienGroupe(6L);

        assertEquals(TypeInvitation.GROUPE, dto.getType());
        assertEquals(StatutInvitation.EN_ATTENTE, dto.getStatut());
        assertEquals(6L, dto.getTontineId());
        assertNull(dto.getNombreParts());
        assertEquals(dto.getCreatedAt().plusDays(7), dto.getExpireAt());
        // 32 octets en Base64 URL sans padding = 43 caractères, sans + / =
        assertEquals(43, dto.getToken().length());
        assertTrue(dto.getToken().matches("[A-Za-z0-9_-]+"));
    }

    // Option A : un seul lien de groupe actif, l'ancien passe en ANNULEE (pas supprimé).
    @Test
    void lienDeGroupe_annuleLAncienLien() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));
        Invitation ancien = invitation(TypeInvitation.GROUPE);
        when(invitationRepository.findByTontineIdAndTypeAndStatut(6L, TypeInvitation.GROUPE,
                StatutInvitation.EN_ATTENTE)).thenReturn(List.of(ancien));
        saveRenvoieLObjet();

        InvitationDTO nouveau = invitationService.genererLienGroupe(6L);

        assertEquals(StatutInvitation.ANNULEE, ancien.getStatut());
        verify(invitationRepository).saveAll(List.of(ancien));
        verify(invitationRepository, never()).delete(any());
        assertNotEquals(ancien.getToken(), nouveau.getToken());
    }

    @Test
    void deuxLiens_ontDesTokensDifferents() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));
        saveRenvoieLObjet();

        String premier = invitationService.genererLienGroupe(6L).getToken();
        String second = invitationService.genererLienGroupe(6L).getToken();

        assertNotEquals(premier, second);
    }

    // ------------------------------------------------ invitation individuelle

    @Test
    void invitationIndividuelle_parUnAutreGestionnaire_estRefusee() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));

        assertThrows(AccesRefuseException.class,
                () -> invitationService.genererInvitationIndividuelle(6L, demande(2)));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void invitationIndividuelle_tontineActive_inscriptionsFermees() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.ACTIVE)));

        assertThrows(InscriptionsFermeesException.class,
                () -> invitationService.genererInvitationIndividuelle(6L, demande(2)));
    }

    @Test
    void invitationIndividuelle_preRemplieAvecSesParts() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));
        saveRenvoieLObjet();

        InvitationDTO dto = invitationService.genererInvitationIndividuelle(6L, demande(2));

        assertEquals(TypeInvitation.INDIVIDUELLE, dto.getType());
        assertEquals(StatutInvitation.EN_ATTENTE, dto.getStatut());
        assertEquals("771234566", dto.getTelephone());
        assertEquals("Awa", dto.getPrenomPreRempli());
        assertEquals("Diop", dto.getNomPreRempli());
        assertEquals(2, dto.getNombreParts());
        assertEquals(dto.getCreatedAt().plusDays(7), dto.getExpireAt());
        assertEquals(43, dto.getToken().length());
    }

    @Test
    void invitationIndividuelle_sansParts_uneParDefaut() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine6(StatutTontine.EN_ATTENTE)));
        saveRenvoieLObjet();

        assertEquals(1, invitationService.genererInvitationIndividuelle(6L, demande(null)).getNombreParts());
    }

    // ------------------------------------------------- consultation du lien

    @Test
    void consulter_tokenInconnu_donne404() {
        when(invitationRepository.findByToken("inconnu")).thenReturn(Optional.empty());

        assertThrows(InvitationIntrouvableException.class, () -> invitationService.consulterLien("inconnu"));
    }

    @Test
    void consulter_lienRemplace_donne410Annulee() {
        Invitation invitation = invitation(TypeInvitation.GROUPE);
        invitation.setStatut(StatutInvitation.ANNULEE);
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));

        assertThrows(InvitationAnnuleeException.class, () -> invitationService.consulterLien("jeton"));
    }

    @Test
    void consulter_invitationDejaUtilisee_donne409() {
        Invitation invitation = invitation(TypeInvitation.INDIVIDUELLE);
        invitation.setStatut(StatutInvitation.ACCEPTE);
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));

        assertThrows(InvitationDejaUtiliseeException.class, () -> invitationService.consulterLien("jeton"));
    }

    // Expiration jugée sur la date : le statut est encore EN_ATTENTE.
    @Test
    void consulter_lienExpire_donne410MemeSiStatutEnAttente() {
        Invitation invitation = invitation(TypeInvitation.GROUPE);
        invitation.setExpireAt(LocalDateTime.now().minusMinutes(1));
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));

        assertThrows(InvitationExpireeException.class, () -> invitationService.consulterLien("jeton"));
    }

    @Test
    void consulter_tontineDemarreeEntreTemps_inscriptionsFermees() {
        Invitation invitation = invitation(TypeInvitation.GROUPE);
        invitation.getTontine().setStatut(StatutTontine.ACTIVE);
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));

        assertThrows(InscriptionsFermeesException.class, () -> invitationService.consulterLien("jeton"));
    }

    @Test
    void consulter_lienValide_renvoieLeNomDeLaTontineEtLePreRemplissage() {
        Invitation invitation = invitation(TypeInvitation.INDIVIDUELLE);
        invitation.setPrenomPreRempli("Awa");
        invitation.setTelephone("771234566");
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));

        LienInvitationDTO dto = invitationService.consulterLien("jeton");

        assertEquals("Natt des femmes", dto.getNomTontine());
        assertEquals(TypeInvitation.INDIVIDUELLE, dto.getType());
        assertEquals("Awa", dto.getPrenomPreRempli());
        assertEquals("771234566", dto.getTelephone());
    }

    // ------------------------------------------------------------ rejoindre

    @Test
    void rejoindre_lienDeGroupe_inscritLeMembreAvecUnePartEtLaisseLeLienValide() {
        connecter("771234566", "ROLE_MEMBRE");
        Invitation lien = invitation(TypeInvitation.GROUPE);
        Utilisateur membre = membre30();
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(lien));
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(membre));

        invitationService.rejoindre("jeton");

        // Le membre vient du token (jamais du client) ; lien de groupe = 1 part.
        verify(participationService).inscrire(lien.getTontine(), membre, 1);
        // Le lien de groupe sert à tout le groupe : il reste EN_ATTENTE.
        assertEquals(StatutInvitation.EN_ATTENTE, lien.getStatut());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void rejoindre_invitationIndividuelle_reprendSesPartsEtPasseEnAccepte() {
        connecter("771234566", "ROLE_MEMBRE");
        Invitation invitation = invitation(TypeInvitation.INDIVIDUELLE);
        invitation.setTelephone("771234566");
        invitation.setNombreParts(2);
        Utilisateur membre = membre30();
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(membre));

        invitationService.rejoindre("jeton");

        verify(participationService).inscrire(invitation.getTontine(), membre, 2);
        assertEquals(StatutInvitation.ACCEPTE, invitation.getStatut());
        verify(invitationRepository).save(invitation);
    }

    // Lien individuel transféré à quelqu'un d'autre : refusé.
    @Test
    void rejoindre_invitationIndividuelle_parUnAutreTelephone_estRefuse() {
        connecter("779999999", "ROLE_MEMBRE");
        Invitation invitation = invitation(TypeInvitation.INDIVIDUELLE);
        invitation.setTelephone("771234566");
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));
        Utilisateur intrus = new Utilisateur();
        intrus.setId(31L);
        intrus.setTelephone("779999999");
        when(utilisateurRepository.findByTelephone("779999999")).thenReturn(Optional.of(intrus));

        assertThrows(AccesRefuseException.class, () -> invitationService.rejoindre("jeton"));
        verifyNoInteractions(participationService);
        assertEquals(StatutInvitation.EN_ATTENTE, invitation.getStatut());
    }

    // L'inscription refuse (membre déjà dans la tontine) : le refus remonte tel quel,
    // et l'invitation à usage unique n'est PAS consommée.
    @Test
    void rejoindre_inscriptionRefusee_donne409SansConsommerLInvitation() {
        connecter("771234566", "ROLE_MEMBRE");
        Invitation invitation = invitation(TypeInvitation.INDIVIDUELLE);
        invitation.setTelephone("771234566");
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));
        when(utilisateurRepository.findByTelephone("771234566")).thenReturn(Optional.of(membre30()));
        when(participationService.inscrire(any(), any(), any()))
                .thenThrow(new ParticipationDejaExistanteException());

        assertThrows(ParticipationDejaExistanteException.class, () -> invitationService.rejoindre("jeton"));
        assertEquals(StatutInvitation.EN_ATTENTE, invitation.getStatut());
        verify(invitationRepository, never()).save(any());
    }

    // Les contrôles du lien s'appliquent aussi à rejoindre (pas seulement à la consultation).
    @Test
    void rejoindre_invitationDejaUtilisee_estRefuseeSansParticipation() {
        connecter("771234566", "ROLE_MEMBRE");
        Invitation invitation = invitation(TypeInvitation.INDIVIDUELLE);
        invitation.setStatut(StatutInvitation.ACCEPTE);
        when(invitationRepository.findByToken("jeton")).thenReturn(Optional.of(invitation));

        assertThrows(InvitationDejaUtiliseeException.class, () -> invitationService.rejoindre("jeton"));
        verifyNoInteractions(participationService);
    }

    // ---------------------------------------------------------------- liste

    @Test
    void liste_neRenvoieQueLesInvitationsDuGestionnaireConnecte() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(invitationRepository.findByTontineGestionnaireTelephoneOrderByCreatedAtDesc("770000101"))
                .thenReturn(List.of(invitation(TypeInvitation.GROUPE)));

        assertEquals(1, invitationService.listInvitation().size());
        verify(invitationRepository, never()).findAll();
    }
}
