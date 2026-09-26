package com.samanatteu.service.tontine;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.dto.tontine.TontineDTO;
import com.samanatteu.entity.Participation;
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.TontineNonModifiableException;
import com.samanatteu.exception.TransitionStatutInvalideException;
import com.samanatteu.repository.ParticipationRepository;
import com.samanatteu.repository.TontineRepository;
import com.samanatteu.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class TontineServiceTest {

    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    // Sert à la lecture "mes tontines" du MEMBRE (les tontines où il a une participation).
    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private TontineService tontineService;

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

        Tontine nouvellesValeurs = new Tontine();
        nouvellesValeurs.setNom("Mbambara");

        assertThrows(AccesRefuseException.class,
                () -> tontineService.updateTontine(6L, nouvellesValeurs));
        verify(tontineRepository, never()).save(any());
    }

    // Scénario du trou de sécurité corrigé : A est connecté, mais le JSON du client
    // désigne B comme
    // gestionnaire. La tontine créée doit appartenir à A (le token), pas à B (le
    // corps de la requête).
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

        // B = le gestionnaire que le client essaie de s'attribuer dans son JSON (id
        // 19L).
        Utilisateur b = new Utilisateur();
        b.setId(19L);
        Tontine demande = new Tontine();
        demande.setGestionnaire(b);
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
        // Attendu = A (18), pas B (19).
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

    // Le client envoie "statut: TERMINEE" dans son JSON : le serveur doit l'ignorer et imposer EN_ATTENTE.
    @Test
    void createTontine_forceLeStatutEnAttente() {
        Utilisateur a = new Utilisateur();
        a.setId(18L);
        a.setTelephone("770000101");
        when(utilisateurRepository.findByTelephone("770000101")).thenReturn(Optional.of(a));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Tontine demande = new Tontine();
        demande.setStatut(StatutTontine.TERMINEE);

        TontineDTO resultat = tontineService.createTontine(demande);

        assertEquals(StatutTontine.EN_ATTENTE, resultat.getStatut());
    }

    // Règle "modifiable seulement en EN_ATTENTE" : une tontine ACTIVE est refusée, et rien n'est enregistré.
    @Test
    void updateTontine_refuseSiLaTontineEstActive() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.ACTIVE)));
        connecterCommeGestionnaire("770000101");

        Tontine nouvellesValeurs = new Tontine();
        nouvellesValeurs.setNom("Nouveau nom");

        assertThrows(TontineNonModifiableException.class,
                () -> tontineService.updateTontine(6L, nouvellesValeurs));
        verify(tontineRepository, never()).save(any());
    }

    // Le client tente de changer le statut et le gestionnaire via le PUT : les deux doivent être ignorés.
    @Test
    void updateTontine_neChangeNiLeStatutNiLeGestionnaire() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Utilisateur b = new Utilisateur();
        b.setId(19L);
        Tontine nouvellesValeurs = new Tontine();
        nouvellesValeurs.setNom("Nouveau nom");
        nouvellesValeurs.setStatut(StatutTontine.TERMINEE);
        nouvellesValeurs.setGestionnaire(b);

        TontineDTO resultat = tontineService.updateTontine(6L, nouvellesValeurs).get();

        // Le nom (champ modifiable) change, mais pas le statut ni le gestionnaire.
        assertEquals("Nouveau nom", resultat.getNom());
        assertEquals(StatutTontine.EN_ATTENTE, resultat.getStatut());
        assertEquals(18L, resultat.getGestionnaireId());
    }

    @Test
    void activerTontine_passeDeEnAttenteAActive() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.EN_ATTENTE)));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TontineDTO resultat = tontineService.activerTontine(6L).get();

        assertEquals(StatutTontine.ACTIVE, resultat.getStatut());
    }

    // La reprise : une tontine suspendue peut être réactivée.
    @Test
    void activerTontine_reprendUneTontineSuspendue() {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontineEnBase(StatutTontine.SUSPENDUE)));
        connecterCommeGestionnaire("770000101");
        when(tontineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TontineDTO resultat = tontineService.activerTontine(6L).get();

        assertEquals(StatutTontine.ACTIVE, resultat.getStatut());
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