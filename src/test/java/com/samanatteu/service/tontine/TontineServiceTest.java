package com.samanatteu.service.tontine;

import static org.mockito.ArgumentMatchers.anyString;
import com.samanatteu.entity.abonnement.Formule;
import com.samanatteu.enums.abonnement.StatutAbonnement;
import com.samanatteu.entity.abonnement.Abonnement;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.samanatteu.dto.tontine.CreationTontineDTO;
import com.samanatteu.dto.tontine.ModificationTontineDTO;
import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.tontine.FrequenceTontine;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.tontine.TontineNonModifiableException;
import com.samanatteu.exception.tontine.TontineSansMembreException;
import com.samanatteu.exception.tontine.TransitionStatutInvalideException;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.repository.utilisateur.UtilisateurRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.abonnement.AbonnementService;

@ExtendWith(MockitoExtension.class)
class TontineServiceTest {

    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    // Sert à la lecture "mes tontines" du MEMBRE (les tontines où il a une participation).
    @Mock
    private ParticipationRepository participationRepository;
    // Un vrai objet (pas un faux) : il lit le SecurityContextHolder rempli par connecterComme...().
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();
    // Un faux : la règle d'abonnement est testée dans AbonnementServiceTest.
    // Ici, on ne teste que la tontine — le faux renvoie une formule MAX
    // (sans quota) pour ne pas bruiter les tests existants.
    @Mock
    private AbonnementService abonnementService;

    @InjectMocks
    private TontineService tontineService;


    // L'abonnement de la gestionnaire : actif, formule MAX (sans quota), pour que
    // la règle d'abonnement ne gene pas les tests de la tontine.
    @BeforeEach
    void abonnementActif() {
        Formule formule = new Formule();
        formule.setNom("MAX");
        formule.setNbTontinesMax(0);
        Abonnement abonnement = new Abonnement();
        abonnement.setFormule(formule);
        abonnement.setStatut(StatutAbonnement.ACTIF);
        abonnement.setDateDebut(java.time.LocalDate.now().minusDays(1));
        abonnement.setDateFin(java.time.LocalDate.now().plusDays(1));
        // lenient : certains tests de cette classe ne creent pas de tontine
        // (ils testent l'activation, la suspension...), et n'utilisent donc pas ce stub.
        lenient().when(abonnementService.exigerAbonnementActif(anyString())).thenReturn(abonnement);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    private void connecterCommeGestionnaire(String telephone) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(telephone, null,
                        List.of(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))));
    }

    private void connecterCommeMembre(String telephone) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(telephone, null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"))));
    }

    @Test
    void deleteTontine_refuseSiPasProprietaire() {
        Utilisateur proprietaire = new Utilisateur();
        proprietaire.setTelephone("770000102");
        Tontine tontine = new Tontine();
        tontine.setGestionnaire(proprietaire);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        connecterCommeGestionnaire("770000101");

        assertThrows(AccesRefuseException.class, () -> tontineService.deleteTontine(6L));

        verify(tontineRepository, never()).deleteById(any());
    }

    @Test
    void deleteTontine_supprimeSiProprietaire() {
        Utilisateur proprietaire = new Utilisateur();
        proprietaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setGestionnaire(proprietaire);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        connecterCommeGestionnaire("770000101");

        boolean resultat = tontineService.deleteTontine(6L);

        assertTrue(resultat);
        verify(tontineRepository).deleteById(6L);

    }

    @Test
    void updateTontine_refuseSiProprietaire() {
        Utilisateur proprietaire = new Utilisateur();
        proprietaire.setTelephone("770000102");
        Tontine tontineEnBase = new Tontine();
        tontineEnBase.setGestionnaire(proprietaire);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase));
        connecterCommeGestionnaire("770000101");

        ModificationTontineDTO nouvellesValeurs = new ModificationTontineDTO();
        nouvellesValeurs.setNom("Mbambara");

        assertThrows(AccesRefuseException.class,
                () -> tontineService.updateTontine(6L, nouvellesValeurs));
        verify(tontineRepository, never()).save(any());
    }

    // Scénario du trou de sécurité corrigé : avant, le JSON pouvait désigner B
    // comme gestionnaire. Avec CreationTontineDTO, ce champ n'existe plus :
    // la tontine créée appartient forcément à A (le token).
    @Test
    void createTontine_imposeLeGestionnaireDuToken() {
        // --- Given (préparer) ---
        // A = la personne connectée. On lui donne un id (18L) car convertiTontineDTO
        // lit
        // getGestionnaire().getId() pour remplir le DTO de sortie.
        Utilisateur a = new Utilisateur();
        a.setId(18L);
        a.setTelephone("770000101");
        // createTontine retrouve le connecté par son téléphone (auth.getName()) : on
        // programme le faux
        // repository pour qu'il réponde "voilà A" à cette recherche précise.
        when(utilisateurRepository.findByTelephone("770000101")).thenReturn(Optional.of(a));
        // On simule le token de A dans le SecurityContextHolder.
        connecterCommeGestionnaire("770000101");

        // Le DTO ne propose aucun champ gestionnaire : impossible de s'attribuer B.
        CreationTontineDTO demande = new CreationTontineDTO();
        demande.setNom("Mbambara");
        demande.setMontantCaissePret(BigDecimal.ZERO);
        demande.setFrequence(FrequenceTontine.MOIS);
        demande.setIntervalle(1);
        // Un mock renvoie null par défaut : sans cette ligne, save() renverrait null et
        // convertiTontineDTO planterait. thenAnswer(...getArgument(0)) = "réponds avec
        // la tontine
        // qu'on t'a donnée", comme un vrai repository qui ne modifie rien.
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // --- When (agir) ---
        TontineDTO resultat = tontineService.createTontine(demande);

        // --- Then (vérifier) ---
        // 18L et pas 18 : getGestionnaireId() est un Long, et assertEquals(int, Long)
        // échouerait.
        // Attendu = A (18) : le token décide, pas le JSON.
        assertEquals(18L, resultat.getGestionnaireId());
    }

    // Protège la règle "un gestionnaire ne voit que SES tontines" : listTontine doit interroger le
    // repository avec le téléphone du token, et jamais avec findAll() (qui renverrait tout à tout le monde).
    @Test
    void listTontine_neRenvoieQueLesTontinesDuConnecte() {
        // --- Given (préparer) ---
        // A (770000101) est connecté : c'est son téléphone que listTontine doit utiliser.
        connecterCommeGestionnaire("770000101");

        // Une tontine de A. Elle a besoin d'un gestionnaire avec un id, car convertiTontineDTO lit
        // getGestionnaire().getId() pour remplir le DTO de sortie.
        Utilisateur a = new Utilisateur();
        a.setId(18L);
        Tontine tontineDeA = new Tontine();
        tontineDeA.setGestionnaire(a);
        // On programme le faux repository UNIQUEMENT pour la méthode filtrée, avec le téléphone de A.
        // Si le service appelait autre chose (findAll par exemple), ce when ne servirait à rien.
        when(tontineRepository.findByGestionnaireTelephone("770000101")).thenReturn(List.of(tontineDeA));

        // --- When (agir) ---
        List<TontineDTO> resultat = tontineService.listTontine();

        // --- Then (vérifier) ---
        // On récupère bien la tontine de A, et une seule.
        assertEquals(1, resultat.size());
        // Et on n'a JAMAIS demandé "toutes les tontines" au repository : c'est ce verify qui
        // attrape la régression si quelqu'un remet findAll() dans listTontine.
        verify(tontineRepository, never()).findAll();
    }

    // Lecture "mes tontines" du MEMBRE : les tontines de SES participations, et pas celles d'un gestionnaire
    // (findByGestionnaireTelephone) ni "toutes" (findAll).
    @Test
    void listTontine_membreVoitLesTontinesOuIlParticipe() {
        connecterCommeMembre("771234566");
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setId(18L);
        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setGestionnaire(gestionnaire);
        Participation participation = new Participation();
        participation.setTontine(tontine);
        when(participationRepository.findByMembreTelephone("771234566")).thenReturn(List.of(participation));

        List<TontineDTO> resultat = tontineService.listTontine();

        assertEquals(1, resultat.size());
        assertEquals(6L, resultat.get(0).getId());
        verify(tontineRepository, never()).findAll();
        verify(tontineRepository, never()).findByGestionnaireTelephone(any());
    }

    // ---------------------------------------------------------------------------------------
    // Cycle de vie du statut (EN_ATTENTE -> ACTIVE <-> SUSPENDUE, ACTIVE -> TERMINEE)
    // ---------------------------------------------------------------------------------------

    // Fabrique une tontine "déjà en base" appartenant à 770000101 (id 18) avec le statut voulu.
    // Le gestionnaire a un id car convertiTontineDTO lit getGestionnaire().getId().
    private Tontine tontineEnBase(StatutTontine statut) {
        Utilisateur proprietaire = new Utilisateur();
        proprietaire.setId(18L);
        proprietaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setGestionnaire(proprietaire);
        tontine.setStatut(statut);
        return tontine;
    }

    // Avant, le client envoyait "statut: TERMINEE" dans son JSON : le serveur devait
    // l'ignorer. Avec le DTO, ce champ n'existe plus : EN_ATTENTE est imposé.
    @Test
    void createTontine_forceLeStatutEnAttente() {
        Utilisateur a = new Utilisateur();
        a.setId(18L);
        a.setTelephone("770000101");
        when(utilisateurRepository.findByTelephone("770000101")).thenReturn(Optional.of(a));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreationTontineDTO demande = new CreationTontineDTO();
        demande.setNom("Mbambara");
        demande.setMontantCaissePret(BigDecimal.ZERO);
        demande.setFrequence(FrequenceTontine.MOIS);
        demande.setIntervalle(1);

        TontineDTO resultat = tontineService.createTontine(demande);

        assertEquals(StatutTontine.EN_ATTENTE, resultat.getStatut());
    }

    // Règle "modifiable seulement en EN_ATTENTE" : une tontine ACTIVE est refusée, et rien n'est enregistré.
    @Test
    void updateTontine_refuseSiLaTontineEstActive() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.ACTIVE)));
        connecterCommeGestionnaire("770000101");

        ModificationTontineDTO nouvellesValeurs = new ModificationTontineDTO();
        nouvellesValeurs.setNom("Nouveau nom");

        assertThrows(TontineNonModifiableException.class,
                () -> tontineService.updateTontine(6L, nouvellesValeurs));
        verify(tontineRepository, never()).save(any());
    }

    // Avec le DTO, statut et gestionnaire n'existent plus dans le PUT : le nom
    // (champ modifiable) change, le reste est forcément conservé.
    @Test
    void updateTontine_neChangeNiLeStatutNiLeGestionnaire() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ModificationTontineDTO nouvellesValeurs = new ModificationTontineDTO();
        nouvellesValeurs.setNom("Nouveau nom");

        TontineDTO resultat = tontineService.updateTontine(6L, nouvellesValeurs).get();

        // Le nom (champ modifiable) change, mais pas le statut ni le gestionnaire.
        assertEquals("Nouveau nom", resultat.getNom());
        assertEquals(StatutTontine.EN_ATTENTE, resultat.getStatut());
        assertEquals(18L, resultat.getGestionnaireId());
    }

    // Membre ACTIF avec n parts (pour le calcul de nbCycles à l'activation).
    private Participation membreActif(int parts) {
        Participation p = new Participation();
        p.setNombreParts(parts);
        p.setStatut(StatutParticipation.ACTIF);
        return p;
    }

    // 1re activation : nbCycles = somme des parts des membres ACTIF (modèle B :
    // une part = un gain = un cycle). Awa 2 + Binta 1 + Coumba 1 = 4 cycles.
    @Test
    void activerTontine_passeDeEnAttenteAActive_etCalculeNbCycles() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000101");
        when(participationRepository.findByTontineIdAndStatut(any(), eq(StatutParticipation.ACTIF)))
                .thenReturn(List.of(membreActif(2), membreActif(1), membreActif(1)));
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TontineDTO resultat = tontineService.activerTontine(6L).get();

        assertEquals(StatutTontine.ACTIVE, resultat.getStatut());
        assertEquals(4, resultat.getNbCycles());
    }

    // Aucun membre : nbCycles vaudrait 0 → refus dès l'activation (409), rien enregistré.
    @Test
    void activerTontine_sansMembre_refuse() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000101");
        when(participationRepository.findByTontineIdAndStatut(any(), eq(StatutParticipation.ACTIF)))
                .thenReturn(List.of());

        assertThrows(TontineSansMembreException.class, () -> tontineService.activerTontine(6L));
        verify(tontineRepository, never()).save(any());
    }

    // La reprise : une tontine suspendue peut être réactivée, SANS recalculer
    // nbCycles (des cycles ont déjà eu lieu, la durée est fixée au lancement).
    @Test
    void activerTontine_reprendUneTontineSuspendue_sansRecalculerNbCycles() {
        Tontine suspendue = tontineEnBase(StatutTontine.SUSPENDUE);
        suspendue.setNbCycles(4);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(suspendue));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TontineDTO resultat = tontineService.activerTontine(6L).get();

        assertEquals(StatutTontine.ACTIVE, resultat.getStatut());
        assertEquals(4, resultat.getNbCycles());
        verify(participationRepository, never()).findByTontineIdAndStatut(any(), any());
    }

    // Avant, le client envoyait "nbCycles: 12" : le serveur l'ignorait et mettait 0
    // (« pas encore calculé »). Avec le DTO, ce champ n'existe plus.
    @Test
    void createTontine_forceNbCyclesAZero() {
        Utilisateur a = new Utilisateur();
        a.setId(18L);
        a.setTelephone("770000101");
        when(utilisateurRepository.findByTelephone("770000101")).thenReturn(Optional.of(a));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreationTontineDTO demande = new CreationTontineDTO();
        demande.setNom("Mbambara");
        demande.setMontantCaissePret(BigDecimal.ZERO);
        demande.setFrequence(FrequenceTontine.MOIS);
        demande.setIntervalle(1);

        TontineDTO resultat = tontineService.createTontine(demande);

        assertEquals(0, resultat.getNbCycles());
    }

    // Le PUT ne touche jamais à nbCycles : le DTO ne le propose pas, le serveur le garde.
    @Test
    void updateTontine_neChangePasNbCycles() {
        Tontine enBase = tontineEnBase(StatutTontine.EN_ATTENTE);
        enBase.setNbCycles(0);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(enBase));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ModificationTontineDTO nouvellesValeurs = new ModificationTontineDTO();
        nouvellesValeurs.setNom("Nouveau nom");

        TontineDTO resultat = tontineService.updateTontine(6L, nouvellesValeurs).get();

        assertEquals(0, resultat.getNbCycles());
    }

    // Caisse de prêts : le serveur part de 0 (sinon on prêterait de l'argent fictif).
    // La règle (montantCaissePret) vient du DTO, l'état (solde) est imposé.
    @Test
    void createTontine_forceLeSoldeDeLaCaisseAZero() {
        Utilisateur a = new Utilisateur();
        a.setId(18L);
        a.setTelephone("770000101");
        when(utilisateurRepository.findByTelephone("770000101")).thenReturn(Optional.of(a));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreationTontineDTO demande = new CreationTontineDTO();
        demande.setNom("Mbambara");
        demande.setMontantCaissePret(new BigDecimal("500"));
        demande.setFrequence(FrequenceTontine.MOIS);
        demande.setIntervalle(1);

        TontineDTO resultat = tontineService.createTontine(demande);

        assertEquals(0, BigDecimal.ZERO.compareTo(resultat.getSoldeCaissePret()));
        assertEquals(0, new BigDecimal("500").compareTo(resultat.getMontantCaissePret()));
    }

    // PUT : la règle (montantCaissePret) change, l'état (soldeCaissePret) jamais
    // (le DTO ne propose pas le solde).
    @Test
    void updateTontine_changeLeMontantDeCaisseMaisPasLeSolde() {
        Tontine enBase = tontineEnBase(StatutTontine.EN_ATTENTE);
        enBase.setSoldeCaissePret(BigDecimal.ZERO);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(enBase));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ModificationTontineDTO nouvellesValeurs = new ModificationTontineDTO();
        nouvellesValeurs.setMontantCaissePret(new BigDecimal("1000"));

        TontineDTO resultat = tontineService.updateTontine(6L, nouvellesValeurs).get();

        assertEquals(0, new BigDecimal("1000").compareTo(resultat.getMontantCaissePret()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resultat.getSoldeCaissePret()));
    }

    // TERMINEE est un état final : on ne ressuscite pas une tontine terminée.
    @Test
    void activerTontine_refuseUneTontineTerminee() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.TERMINEE)));
        connecterCommeGestionnaire("770000101");

        assertThrows(TransitionStatutInvalideException.class, () -> tontineService.activerTontine(6L));
        verify(tontineRepository, never()).save(any());
    }

    @Test
    void suspendreTontine_passeDeActiveASuspendue() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.ACTIVE)));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TontineDTO resultat = tontineService.suspendreTontine(6L).get();

        assertEquals(StatutTontine.SUSPENDUE, resultat.getStatut());
    }

    @Test
    void suspendreTontine_refuseUneTontineEnAttente() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000101");

        assertThrows(TransitionStatutInvalideException.class, () -> tontineService.suspendreTontine(6L));
        verify(tontineRepository, never()).save(any());
    }

    @Test
    void cloturerTontine_passeDeActiveATerminee() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.ACTIVE)));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TontineDTO resultat = tontineService.cloturerTontine(6L).get();

        assertEquals(StatutTontine.TERMINEE, resultat.getStatut());
    }

    @Test
    void cloturerTontine_refuseUneTontineEnAttente() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000101");

        assertThrows(TransitionStatutInvalideException.class, () -> tontineService.cloturerTontine(6L));
        verify(tontineRepository, never()).save(any());
    }

    // Un autre gestionnaire (770000102) ne peut pas changer le statut de la tontine de 770000101.
    @Test
    void activerTontine_refuseSiPasProprietaire() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000102");

        assertThrows(AccesRefuseException.class, () -> tontineService.activerTontine(6L));
        verify(tontineRepository, never()).save(any());
    }

    // Tontine inexistante : Optional vide (le contrôleur en fera un 404), pas d'exception.
    @Test
    void activerTontine_renvoieVideSiLaTontineNExistePas() {
        when(tontineRepository.findById(99L)).thenReturn(Optional.empty());
        connecterCommeGestionnaire("770000101");

        assertTrue(tontineService.activerTontine(99L).isEmpty());
    }

}