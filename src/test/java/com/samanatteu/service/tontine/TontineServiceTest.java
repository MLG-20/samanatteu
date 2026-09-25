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
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.repository.TontineRepository;
import com.samanatteu.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class TontineServiceTest {

    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

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

}