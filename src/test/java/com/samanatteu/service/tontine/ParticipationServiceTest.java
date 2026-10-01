package com.samanatteu.service.tontine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.dto.tontine.ParticipationDTO;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.tontine.InscriptionsFermeesException;
import com.samanatteu.exception.tontine.NombrePartsInvalideException;
import com.samanatteu.exception.tontine.ParticipationDejaExistanteException;
import com.samanatteu.exception.tontine.RelationObligatoireException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.exception.utilisateur.MembreIntrouvableException;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Tests des règles de ParticipationService, avec de faux repositories (Mockito) : ni base ni serveur.
// Convention des données : le gestionnaire propriétaire de la tontine 6 a le téléphone 770000101 ;
// un "étranger" est 770000102.
@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private TontineRepository tontineRepository;
    // Sert à vérifier que le membre existe (le membre du JSON n'est qu'un {id} fabriqué par le client).
    @Mock
    private UtilisateurRepository utilisateurRepository;
    // Un vrai objet (pas un faux) : il lit le SecurityContextHolder rempli par connecterComme...().
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private ParticipationService participationService;

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

    // La tontine 6, propriété de 770000101, avec le statut voulu.
    private Tontine tontineDe770000101(StatutTontine statut) {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setId(18L);
        gestionnaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setGestionnaire(gestionnaire);
        tontine.setStatut(statut);
        return tontine;
    }

    // Le vrai membre 30, tel qu'il existe en base.
    private Utilisateur membre30() {
        Utilisateur membre = new Utilisateur();
        membre.setId(30L);
        return membre;
    }

    // Ce que le client envoie pour inscrire le membre 30 à la tontine 6 avec ce nombre de parts.
    // Le JSON du client ne contient qu'un {id: 6} pour la tontine (objet "fabriqué").
    private Participation demande(Integer parts) {
        Utilisateur membre = new Utilisateur();
        membre.setId(30L);
        Tontine tontineDuJson = new Tontine();
        tontineDuJson.setId(6L);
        Participation p = new Participation();
        p.setMembre(membre);
        p.setTontine(tontineDuJson);
        p.setNombreParts(parts);
        return p;
    }

    // Une participation déjà en base (id 9) du membre 30 dans la tontine donnée.
    private Participation participationEnBase(Tontine tontine, int parts) {
        Utilisateur membre = new Utilisateur();
        membre.setId(30L);
        Participation p = new Participation();
        p.setId(9L);
        p.setMembre(membre);
        p.setTontine(tontine);
        p.setNombreParts(parts);
        p.setStatut(StatutParticipation.ACTIF);
        p.setOrdreInscription(1);
        return p;
    }

    // ------------------------------------------------- création : les refus

    @Test
    void createParticipation_refuseSansMembre() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Participation sansMembre = demande(2);
        sansMembre.setMembre(null);

        assertThrows(RelationObligatoireException.class,
                () -> participationService.createParticipation(sansMembre));
        verify(participationRepository, never()).save(any());
    }

    @Test
    void createParticipation_refuseSansTontine() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Participation sansTontine = demande(2);
        sansTontine.setTontine(null);

        assertThrows(RelationObligatoireException.class,
                () -> participationService.createParticipation(sansTontine));
        verify(participationRepository, never()).save(any());
    }

    @Test
    void createParticipation_renvoie404SiLaTontineNExistePas() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.empty());

        assertThrows(TontineIntrouvableException.class,
                () -> participationService.createParticipation(demande(2)));
        verify(participationRepository, never()).save(any());
    }

    // Un autre gestionnaire ne peut pas inscrire quelqu'un dans la tontine de 770000101.
    @Test
    void createParticipation_refuseSiPasProprietaire() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineDe770000101(StatutTontine.EN_ATTENTE)));
        connecter("770000102", "ROLE_GESTIONNAIRE");

        assertThrows(AccesRefuseException.class,
                () -> participationService.createParticipation(demande(2)));
        verify(participationRepository, never()).save(any());
    }

    @Test
    void createParticipation_refuseUnDoublon() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineDe770000101(StatutTontine.EN_ATTENTE)));
        when(utilisateurRepository.findById(30L)).thenReturn(Optional.of(membre30()));
        when(participationRepository.existsByMembreIdAndTontineId(30L, 6L)).thenReturn(true);
        connecter("770000101", "ROLE_GESTIONNAIRE");

        assertThrows(ParticipationDejaExistanteException.class,
                () -> participationService.createParticipation(demande(2)));
        verify(participationRepository, never()).save(any());
    }

    // Une fois la tontine lancée, on n'inscrit plus personne (cela fausserait le tirage).
    @Test
    void createParticipation_refuseSiLaTontineEstActive() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineDe770000101(StatutTontine.ACTIVE)));
        when(utilisateurRepository.findById(30L)).thenReturn(Optional.of(membre30()));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        assertThrows(InscriptionsFermeesException.class,
                () -> participationService.createParticipation(demande(2)));
        verify(participationRepository, never()).save(any());
    }

    // null, 0 et -3 sont tous refusés : @NullSource ajoute le cas null à la liste des valeurs.
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = { 0, -3 })
    void createParticipation_refuseUnNombreDePartsInvalide(Integer parts) {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineDe770000101(StatutTontine.EN_ATTENTE)));
        when(utilisateurRepository.findById(30L)).thenReturn(Optional.of(membre30()));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        assertThrows(NombrePartsInvalideException.class,
                () -> participationService.createParticipation(demande(parts)));
        verify(participationRepository, never()).save(any());
    }

    // Un {"membre": {}} sans id : refusé proprement (400) au lieu de planter plus loin (500).
    @Test
    void createParticipation_refuseUnMembreSansId() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Participation membreSansId = demande(2);
        membreSansId.setMembre(new Utilisateur());

        assertThrows(RelationObligatoireException.class,
                () -> participationService.createParticipation(membreSansId));
        verify(participationRepository, never()).save(any());
    }

    @Test
    void createParticipation_refuseUneTontineSansId() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Participation tontineSansId = demande(2);
        tontineSansId.setTontine(new Tontine());

        assertThrows(RelationObligatoireException.class,
                () -> participationService.createParticipation(tontineSansId));
        verify(participationRepository, never()).save(any());
    }

    // Le membre du JSON n'existe pas en base : 404 propre, et rien n'est enregistré
    // (avant ce contrôle, la base refusait la clé étrangère et le client recevait un 500).
    @Test
    void createParticipation_renvoie404SiLeMembreNExistePas() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineDe770000101(StatutTontine.EN_ATTENTE)));
        when(utilisateurRepository.findById(30L)).thenReturn(Optional.empty());
        connecter("770000101", "ROLE_GESTIONNAIRE");

        assertThrows(MembreIntrouvableException.class,
                () -> participationService.createParticipation(demande(2)));
        verify(participationRepository, never()).save(any());
    }

    // Le contrôle du propriétaire passe AVANT la recherche du membre : un étranger reçoit 403, sans que le
    // service n'aille même regarder si le membre existe.
    @Test
    void createParticipation_lEtrangerEstRefuseAvantLaRechercheDuMembre() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineDe770000101(StatutTontine.EN_ATTENTE)));
        connecter("770000102", "ROLE_GESTIONNAIRE");

        assertThrows(AccesRefuseException.class,
                () -> participationService.createParticipation(demande(2)));
        verify(utilisateurRepository, never()).findById(any());
    }

    // ---------------------------------------- création : ce que décide le serveur

    // Première inscription : ordre 1, statut ACTIF, date du jour, et le client ne peut pas imposer ses valeurs.
    @Test
    void createParticipation_lePremierInscritRecoitLOrdre1() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineDe770000101(StatutTontine.EN_ATTENTE)));
        when(utilisateurRepository.findById(30L)).thenReturn(Optional.of(membre30()));
        when(participationRepository.findFirstByTontineIdOrderByOrdreInscriptionDesc(6L))
                .thenReturn(Optional.empty());
        when(participationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        // Le client essaie de tricher : il envoie un statut et un ordre de son choix.
        Participation triche = demande(2);
        triche.setStatut(StatutParticipation.SORTI);
        triche.setOrdreInscription(99);

        ParticipationDTO resultat = participationService.createParticipation(triche);

        assertEquals(1, resultat.getOrdreInscription());
        assertEquals(StatutParticipation.ACTIF, resultat.getStatut());
        assertNotNull(resultat.getDateAdhesion());
        assertEquals(2, resultat.getNombreParts());
        assertEquals(6L, resultat.getTontineId());
        assertEquals(30L, resultat.getMembreId());
    }

    // "dernier + 1" et non "nombre d'inscrits + 1" : si le plus grand ordre déjà pris est 2, le suivant est 3.
    @Test
    void createParticipation_donneLeNumeroSuivantLeDernierOrdre() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        when(utilisateurRepository.findById(30L)).thenReturn(Optional.of(membre30()));
        Participation derniere = participationEnBase(tontine, 1);
        derniere.setOrdreInscription(2);
        when(participationRepository.findFirstByTontineIdOrderByOrdreInscriptionDesc(6L))
                .thenReturn(Optional.of(derniere));
        when(participationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        ParticipationDTO resultat = participationService.createParticipation(demande(1));

        assertEquals(3, resultat.getOrdreInscription());
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateParticipation_neChangeQueLeNombreDeParts() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(participationRepository.findById(9L)).thenReturn(Optional.of(participationEnBase(tontine, 1)));
        when(participationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        // Le client tente aussi de déplacer la participation vers un autre membre et de changer son statut.
        Utilisateur autreMembre = new Utilisateur();
        autreMembre.setId(31L);
        Participation modif = new Participation();
        modif.setNombreParts(5);
        modif.setMembre(autreMembre);
        modif.setStatut(StatutParticipation.SORTI);

        ParticipationDTO resultat = participationService.updateParticipation(9L, modif).get();

        assertEquals(5, resultat.getNombreParts());
        assertEquals(30L, resultat.getMembreId());
        assertEquals(StatutParticipation.ACTIF, resultat.getStatut());
    }

    @Test
    void updateParticipation_refuseSiPasProprietaire() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(participationRepository.findById(9L)).thenReturn(Optional.of(participationEnBase(tontine, 1)));
        connecter("770000102", "ROLE_GESTIONNAIRE");

        Participation modif = new Participation();
        modif.setNombreParts(5);

        assertThrows(AccesRefuseException.class, () -> participationService.updateParticipation(9L, modif));
        verify(participationRepository, never()).save(any());
    }

    @Test
    void updateParticipation_refuseSiLaTontineEstActive() {
        Tontine tontine = tontineDe770000101(StatutTontine.ACTIVE);
        when(participationRepository.findById(9L)).thenReturn(Optional.of(participationEnBase(tontine, 1)));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        Participation modif = new Participation();
        modif.setNombreParts(5);

        assertThrows(InscriptionsFermeesException.class, () -> participationService.updateParticipation(9L, modif));
        verify(participationRepository, never()).save(any());
    }

    @Test
    void updateParticipation_refuseUnNombreDePartsInvalide() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(participationRepository.findById(9L)).thenReturn(Optional.of(participationEnBase(tontine, 1)));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        Participation modif = new Participation();
        modif.setNombreParts(0);

        assertThrows(NombrePartsInvalideException.class, () -> participationService.updateParticipation(9L, modif));
        verify(participationRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteParticipation_supprimeSiProprietaireEtTontineEnAttente() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(participationRepository.findById(9L)).thenReturn(Optional.of(participationEnBase(tontine, 1)));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        assertTrue(participationService.deleteParticipation(9L));
        verify(participationRepository).deleteById(9L);
    }

    @Test
    void deleteParticipation_refuseSiPasProprietaire() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(participationRepository.findById(9L)).thenReturn(Optional.of(participationEnBase(tontine, 1)));
        connecter("770000102", "ROLE_GESTIONNAIRE");

        assertThrows(AccesRefuseException.class, () -> participationService.deleteParticipation(9L));
        verify(participationRepository, never()).deleteById(any());
    }

    @Test
    void deleteParticipation_refuseSiLaTontineEstActive() {
        Tontine tontine = tontineDe770000101(StatutTontine.ACTIVE);
        when(participationRepository.findById(9L)).thenReturn(Optional.of(participationEnBase(tontine, 1)));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        assertThrows(InscriptionsFermeesException.class, () -> participationService.deleteParticipation(9L));
        verify(participationRepository, never()).deleteById(any());
    }

    @Test
    void deleteParticipation_renvoieFauxSiElleNExistePas() {
        when(participationRepository.findById(99L)).thenReturn(Optional.empty());
        connecter("770000101", "ROLE_GESTIONNAIRE");

        assertFalse(participationService.deleteParticipation(99L));
        verify(participationRepository, never()).deleteById(any());
    }

    // ---------------------------------------------------------- lecture filtrée

    // Le gestionnaire voit les participations de SES tontines, jamais "tout" (findAll).
    @Test
    void listParticipation_gestionnaireVoitCellesDeSesTontines() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(participationRepository.findByTontineGestionnaireTelephone("770000101"))
                .thenReturn(List.of(participationEnBase(tontine, 1)));
        connecter("770000101", "ROLE_GESTIONNAIRE");

        List<ParticipationDTO> resultat = participationService.listParticipation();

        assertEquals(1, resultat.size());
        verify(participationRepository, never()).findAll();
        verify(participationRepository, never()).findByMembreTelephone(any());
    }

    // Le membre ne voit que SES participations.
    @Test
    void listParticipation_membreVoitSeulementLesSiennes() {
        Tontine tontine = tontineDe770000101(StatutTontine.EN_ATTENTE);
        when(participationRepository.findByMembreTelephone("771234566"))
                .thenReturn(List.of(participationEnBase(tontine, 1)));
        connecter("771234566", "ROLE_MEMBRE");

        List<ParticipationDTO> resultat = participationService.listParticipation();

        assertEquals(1, resultat.size());
        verify(participationRepository, never()).findAll();
        verify(participationRepository, never()).findByTontineGestionnaireTelephone(any());
    }
}
