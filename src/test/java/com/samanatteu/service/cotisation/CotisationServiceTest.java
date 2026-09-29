package com.samanatteu.service.cotisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

import com.samanatteu.dto.cotisation.CotisationDTO;
import com.samanatteu.dto.cotisation.PaiementDTO;
import com.samanatteu.entity.Cotisation;
import com.samanatteu.entity.Cycle;
import com.samanatteu.entity.Participation;
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.ModePaiementCotisation;
import com.samanatteu.enums.StatutCotisation;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.CotisationDejaPayeeException;
import com.samanatteu.exception.CotisationIntrouvableException;
import com.samanatteu.exception.MontantPayeSuperieurAuDuException;
import com.samanatteu.repository.CotisationRepository;
import com.samanatteu.repository.CycleRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Tests des règles de CotisationService (paiement, lecture filtrée), avec de faux repositories.
// Convention : la cotisation 7 (dû 10 000 F) appartient au cycle 41 de la tontine 6, gérée par
// 770000101 ; un "étranger" est 770000102.
@ExtendWith(MockitoExtension.class)
class CotisationServiceTest {

    @Mock
    private CotisationRepository cotisationRepository;
    @Mock
    private CycleRepository cycleRepository;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private CotisationService cotisationService;

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

    // Cotisation 7 : dû 10 000, déjà payé "paye", avec ce statut ; son cycle a déjà collecté "collecte".
    private Cotisation cotisation(String paye, StatutCotisation statut, String collecte) {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setGestionnaire(gestionnaire);
        Cycle cycle = new Cycle();
        cycle.setId(41L);
        cycle.setTontine(tontine);
        cycle.setMontantCollecte(new BigDecimal(collecte));
        Participation participation = new Participation();
        participation.setId(9L);

        Cotisation c = new Cotisation();
        c.setId(7L);
        c.setCycle(cycle);
        c.setParticipation(participation);
        c.setMontantDu(new BigDecimal("10000.00"));
        c.setMontantPaye(new BigDecimal(paye));
        c.setStatut(statut);
        return c;
    }

    private PaiementDTO paiement(String montant) {
        PaiementDTO p = new PaiementDTO();
        p.setMontant(new BigDecimal(montant));
        p.setModePaiement(ModePaiementCotisation.WAVE);
        p.setReference("W-123");
        return p;
    }

    private CotisationDTO payer(Cotisation cotisation, String montant) {
        when(cotisationRepository.findById(7L)).thenReturn(Optional.of(cotisation));
        when(cotisationRepository.save(cotisation)).thenReturn(cotisation);
        return cotisationService.enregistrerPaiement(7L, paiement(montant));
    }

    // ---------------------------------------------------------------- refus

    @Test
    void payer_cotisationInexistante_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cotisationRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(CotisationIntrouvableException.class,
                () -> cotisationService.enregistrerPaiement(7L, paiement("1000")));
    }

    @Test
    void payer_parUnEtranger_estRefuseSansRienEnregistrer() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(cotisationRepository.findById(7L))
                .thenReturn(Optional.of(cotisation("0", StatutCotisation.EN_ATTENTE, "0")));

        assertThrows(AccesRefuseException.class,
                () -> cotisationService.enregistrerPaiement(7L, paiement("1000")));
        verify(cotisationRepository, never()).save(any());
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void payer_uneCotisationDejaComplete_estRefuse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cotisationRepository.findById(7L))
                .thenReturn(Optional.of(cotisation("10000", StatutCotisation.COMPLET, "10000")));

        assertThrows(CotisationDejaPayeeException.class,
                () -> cotisationService.enregistrerPaiement(7L, paiement("1")));
    }

    // Dû 10 000, déjà payé 6 000 : il reste 4 000, un paiement de 5 000 est refusé.
    @Test
    void payer_plusQueLeResteDu_estRefuseSansRienEnregistrer() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cotisationRepository.findById(7L))
                .thenReturn(Optional.of(cotisation("6000", StatutCotisation.PARTIEL, "6000")));

        assertThrows(MontantPayeSuperieurAuDuException.class,
                () -> cotisationService.enregistrerPaiement(7L, paiement("5000")));
        verify(cotisationRepository, never()).save(any());
        verify(cycleRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- succès

    @Test
    void payer_unePartie_passePartielEtEnregistreLesDetails() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cotisation c = cotisation("0", StatutCotisation.EN_ATTENTE, "0");

        CotisationDTO resultat = payer(c, "3000");

        assertEquals(StatutCotisation.PARTIEL, resultat.getStatut());
        assertEquals(0, new BigDecimal("3000").compareTo(resultat.getMontantPaye()));
        assertEquals(ModePaiementCotisation.WAVE, resultat.getModePaiement());
        assertEquals("W-123", resultat.getReference());
        assertNotNull(resultat.getDatePaiement());
    }

    // 6000 + 4000 = « 10000 » (0 décimale), le dû vaut « 10000.00 » (2 décimales) : equals()
    // les dirait différents, compareTo() les dit égaux. C'est pour ça qu'on compare avec compareTo.
    @Test
    void payer_exactementLeReste_passeComplet() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cotisation c = cotisation("6000", StatutCotisation.PARTIEL, "6000");

        assertEquals(StatutCotisation.COMPLET, payer(c, "4000").getStatut());
    }

    // Un paiement partiel n'efface pas le retard.
    @Test
    void payer_unePartieDUneCotisationEnRetard_resteEnRetard() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cotisation c = cotisation("0", StatutCotisation.EN_RETARD, "0");

        assertEquals(StatutCotisation.EN_RETARD, payer(c, "3000").getStatut());
    }

    // Option B : un retardataire peut solder après la clôture.
    @Test
    void payer_toutLeResteDUneCotisationEnRetard_passeComplet() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cotisation c = cotisation("4000", StatutCotisation.EN_RETARD, "4000");

        assertEquals(StatutCotisation.COMPLET, payer(c, "6000").getStatut());
    }

    // Le cycle encaisse seulement l'argent qui arrive : 6 000 + 4 000 = 10 000
    // (et non 6 000 + le nouveau total payé de 10 000).
    @Test
    void payer_augmenteLeMontantCollecteDuCycleDuSeulPaiement() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cotisation c = cotisation("6000", StatutCotisation.PARTIEL, "6000");

        payer(c, "4000");

        assertEquals(0, new BigDecimal("10000").compareTo(c.getCycle().getMontantCollecte()));
        verify(cycleRepository).save(c.getCycle());
    }

    // -------------------------------------------------------- lecture filtrée

    @Test
    void lister_parUnGestionnaire_passeParSesTontines() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cotisationRepository.findByCycleTontineGestionnaireTelephone("770000101"))
                .thenReturn(List.of(cotisation("0", StatutCotisation.EN_ATTENTE, "0")));

        assertEquals(1, cotisationService.listCotisations().size());
        verify(cotisationRepository, never()).findAll();
        verify(cotisationRepository, never()).findByParticipationMembreTelephone(any());
    }

    @Test
    void lister_parUnMembre_neRenvoieQueSesCotisations() {
        connecter("771234566", "ROLE_MEMBRE");
        when(cotisationRepository.findByParticipationMembreTelephone("771234566"))
                .thenReturn(List.of(cotisation("0", StatutCotisation.EN_ATTENTE, "0")));

        assertEquals(1, cotisationService.listCotisations().size());
        verify(cotisationRepository, never()).findAll();
        verify(cotisationRepository, never()).findByCycleTontineGestionnaireTelephone(any());
    }

    // ------------------------------------------------------------ suppression

    @Test
    void supprimer_parUnEtranger_estRefuseSansSupprimer() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(cotisationRepository.findById(7L))
                .thenReturn(Optional.of(cotisation("0", StatutCotisation.EN_ATTENTE, "0")));

        assertThrows(AccesRefuseException.class, () -> cotisationService.deleteCotisation(7L));
        verify(cotisationRepository, never()).deleteById(any());
    }
}
