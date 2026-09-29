package com.samanatteu.service.tontine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.dto.tontine.CycleDTO;
import com.samanatteu.entity.Cotisation;
import com.samanatteu.entity.Cycle;
import com.samanatteu.entity.Participation;
import com.samanatteu.entity.Tontine;
import com.samanatteu.entity.Utilisateur;
import com.samanatteu.enums.FrequenceTontine;
import com.samanatteu.enums.StatutCotisation;
import com.samanatteu.enums.StatutCycle;
import com.samanatteu.enums.StatutParticipation;
import com.samanatteu.enums.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.tontine.CycleDejaEnCoursException;
import com.samanatteu.exception.tontine.CycleIntrouvableException;
import com.samanatteu.exception.tontine.CycleNonEnCoursException;
import com.samanatteu.exception.tontine.NombreCyclesAtteintException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.exception.tontine.TontineNonActiveException;
import com.samanatteu.repository.CotisationRepository;
import com.samanatteu.repository.CycleRepository;
import com.samanatteu.repository.ParticipationRepository;
import com.samanatteu.repository.TontineRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Tests des règles de CycleService (ouverture, clôture, lecture filtrée), avec de faux repositories.
// Convention : la tontine 6 appartient au gestionnaire 770000101 ; un "étranger" est 770000102.
@ExtendWith(MockitoExtension.class)
class CycleServiceTest {

    @Mock
    private CycleRepository cycleRepository;
    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private CotisationRepository cotisationRepository;
    // Un vrai objet : il lit le SecurityContextHolder rempli par connecter().
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private CycleService cycleService;

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

    // Tontine 6 de 770000101 : 5 000 F la part, tous les 2 mois, 3 cycles prévus.
    private Tontine tontine(StatutTontine statut) {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setGestionnaire(gestionnaire);
        tontine.setStatut(statut);
        tontine.setMontantPart(new BigDecimal("5000"));
        tontine.setFrequence(FrequenceTontine.MOIS);
        tontine.setIntervalle(2);
        tontine.setNbCycles(3);
        return tontine;
    }

    private Participation participation(Tontine tontine, int parts) {
        Participation p = new Participation();
        p.setTontine(tontine);
        p.setNombreParts(parts);
        p.setStatut(StatutParticipation.ACTIF);
        return p;
    }

    private Cycle cycle(Tontine tontine, int numero, StatutCycle statut) {
        Cycle cycle = new Cycle();
        cycle.setId(40L + numero);
        cycle.setTontine(tontine);
        cycle.setNumeroCycle(numero);
        cycle.setStatut(statut);
        return cycle;
    }

    private Cotisation cotisation(StatutCotisation statut) {
        Cotisation c = new Cotisation();
        c.setStatut(statut);
        return c;
    }

    // Le faux save() renvoie l'objet reçu, avec un id (comme la vraie base).
    private void saveDuCycleRenvoieLeCycle() {
        when(cycleRepository.save(any(Cycle.class))).thenAnswer(appel -> {
            Cycle c = appel.getArgument(0);
            c.setId(41L);
            return c;
        });
    }

    // ------------------------------------------------------- ouverture : refus

    @Test
    void ouvrir_tontineInexistante_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.empty());

        assertThrows(TontineIntrouvableException.class, () -> cycleService.ouvrirCycle(6L));
    }

    @Test
    void ouvrir_parUnEtranger_estRefuseSansRienEnregistrer() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine(StatutTontine.ACTIVE)));

        assertThrows(AccesRefuseException.class, () -> cycleService.ouvrirCycle(6L));
        verify(cycleRepository, never()).save(any());
        verify(cotisationRepository, never()).save(any());
    }

    @Test
    void ouvrir_tontineEnAttente_estRefuse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine(StatutTontine.EN_ATTENTE)));

        assertThrows(TontineNonActiveException.class, () -> cycleService.ouvrirCycle(6L));
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void ouvrir_alorsQuUnCycleEstEnCours_estRefuse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine(StatutTontine.ACTIVE)));
        when(cycleRepository.existsByTontineIdAndStatut(6L, StatutCycle.EN_COURS)).thenReturn(true);

        assertThrows(CycleDejaEnCoursException.class, () -> cycleService.ouvrirCycle(6L));
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void ouvrir_auDelaDuNombreDeCyclesPrevus_estRefuse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine(StatutTontine.ACTIVE);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        // Le dernier cycle est le 3e, et la tontine en prévoit 3.
        when(cycleRepository.findFirstByTontineIdOrderByNumeroCycleDesc(6L))
                .thenReturn(Optional.of(cycle(tontine, 3, StatutCycle.CLOTURE)));

        assertThrows(NombreCyclesAtteintException.class, () -> cycleService.ouvrirCycle(6L));
        verify(cycleRepository, never()).save(any());
    }

    // ------------------------------------------------------ ouverture : succès

    @Test
    void ouvrir_premierCycle_calculeToutCoteServeur() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine(StatutTontine.ACTIVE);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        // 3 membres actifs : 1, 2 et 1 parts.
        when(participationRepository.findByTontineIdAndStatut(6L, StatutParticipation.ACTIF))
                .thenReturn(List.of(participation(tontine, 1), participation(tontine, 2),
                        participation(tontine, 1)));
        saveDuCycleRenvoieLeCycle();

        CycleDTO resultat = cycleService.ouvrirCycle(6L);

        assertEquals(1, resultat.getNumeroCycle());
        assertEquals(StatutCycle.EN_COURS, resultat.getStatut());
        assertEquals(LocalDate.now(), resultat.getDateDebut());
        assertEquals(LocalDate.now().plusMonths(2), resultat.getDateFinPrevue());
        assertEquals(0, BigDecimal.ZERO.compareTo(resultat.getMontantCollecte()));
        // 5 000 + 10 000 + 5 000
        assertEquals(0, new BigDecimal("20000").compareTo(resultat.getMontantAttendu()));

        // Une cotisation par membre actif, montantDu = parts × montantPart, rien de payé.
        ArgumentCaptor<Cotisation> captees = ArgumentCaptor.forClass(Cotisation.class);
        verify(cotisationRepository, times(3)).save(captees.capture());
        List<BigDecimal> dus = captees.getAllValues().stream().map(Cotisation::getMontantDu).toList();
        assertEquals(0, new BigDecimal("5000").compareTo(dus.get(0)));
        assertEquals(0, new BigDecimal("10000").compareTo(dus.get(1)));
        assertEquals(0, new BigDecimal("5000").compareTo(dus.get(2)));
        for (Cotisation c : captees.getAllValues()) {
            assertEquals(StatutCotisation.EN_ATTENTE, c.getStatut());
            assertEquals(0, BigDecimal.ZERO.compareTo(c.getMontantPaye()));
            assertEquals(41L, c.getCycle().getId());
        }
    }

    @Test
    void ouvrir_leNumeroEstLeDernierPlusUn() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine(StatutTontine.ACTIVE);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        when(cycleRepository.findFirstByTontineIdOrderByNumeroCycleDesc(6L))
                .thenReturn(Optional.of(cycle(tontine, 2, StatutCycle.CLOTURE)));
        saveDuCycleRenvoieLeCycle();

        assertEquals(3, cycleService.ouvrirCycle(6L).getNumeroCycle());
    }

    // Fin prévue = début + intervalle unités, pour chaque unité.
    @ParameterizedTest
    @CsvSource({ "JOUR, 15, 15", "SEMAINE, 1, 7", "SEMAINE, 2, 14" })
    void ouvrir_laFinPrevueSuitLaFrequence(FrequenceTontine frequence, int intervalle, int jours) {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine(StatutTontine.ACTIVE);
        tontine.setFrequence(frequence);
        tontine.setIntervalle(intervalle);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        saveDuCycleRenvoieLeCycle();

        assertEquals(LocalDate.now().plusDays(jours), cycleService.ouvrirCycle(6L).getDateFinPrevue());
    }

    // --------------------------------------------------------------- clôture

    @Test
    void cloturer_cycleInexistant_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(41L)).thenReturn(Optional.empty());

        assertThrows(CycleIntrouvableException.class, () -> cycleService.cloturerCycle(41L));
    }

    @Test
    void cloturer_parUnEtranger_estRefuse() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(41L))
                .thenReturn(Optional.of(cycle(tontine(StatutTontine.ACTIVE), 1, StatutCycle.EN_COURS)));

        assertThrows(AccesRefuseException.class, () -> cycleService.cloturerCycle(41L));
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void cloturer_unCycleDejaCloture_estRefuse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(41L))
                .thenReturn(Optional.of(cycle(tontine(StatutTontine.ACTIVE), 1, StatutCycle.CLOTURE)));

        assertThrows(CycleNonEnCoursException.class, () -> cycleService.cloturerCycle(41L));
        verify(cotisationRepository, never()).save(any());
    }

    // Option B : les impayés (EN_ATTENTE, PARTIEL) passent EN_RETARD, les COMPLET ne bougent pas.
    @Test
    void cloturer_passeLesImpayesEnRetardEtFermeLeCycle() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cycle cycle = cycle(tontine(StatutTontine.ACTIVE), 1, StatutCycle.EN_COURS);
        when(cycleRepository.findById(41L)).thenReturn(Optional.of(cycle));
        Cotisation enAttente = cotisation(StatutCotisation.EN_ATTENTE);
        Cotisation partielle = cotisation(StatutCotisation.PARTIEL);
        Cotisation complete = cotisation(StatutCotisation.COMPLET);
        when(cotisationRepository.findByCycleId(41L)).thenReturn(List.of(enAttente, partielle, complete));
        when(cycleRepository.save(cycle)).thenReturn(cycle);

        CycleDTO resultat = cycleService.cloturerCycle(41L);

        assertEquals(StatutCotisation.EN_RETARD, enAttente.getStatut());
        assertEquals(StatutCotisation.EN_RETARD, partielle.getStatut());
        assertEquals(StatutCotisation.COMPLET, complete.getStatut());
        verify(cotisationRepository, never()).save(complete);
        assertEquals(StatutCycle.CLOTURE, resultat.getStatut());
        assertEquals(LocalDate.now(), resultat.getDateFinReelle());
    }

    // -------------------------------------------------------- lecture filtrée

    @Test
    void lister_parUnGestionnaire_neRenvoieQueSesCycles() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findByTontineGestionnaireTelephone("770000101"))
                .thenReturn(List.of(cycle(tontine(StatutTontine.ACTIVE), 1, StatutCycle.EN_COURS)));

        assertEquals(1, cycleService.listCycle().size());
        verify(cycleRepository, never()).findAll();
        verify(cycleRepository, never()).findByTontineIdIn(anyList());
    }

    @Test
    void lister_parUnMembre_passeParLesTontinesOuIlParticipe() {
        connecter("771234566", "ROLE_MEMBRE");
        Tontine tontine = tontine(StatutTontine.ACTIVE);
        when(participationRepository.findByMembreTelephone("771234566"))
                .thenReturn(List.of(participation(tontine, 1)));
        when(cycleRepository.findByTontineIdIn(List.of(6L)))
                .thenReturn(List.of(cycle(tontine, 1, StatutCycle.EN_COURS)));

        assertEquals(1, cycleService.listCycle().size());
        verify(cycleRepository, never()).findAll();
        verify(cycleRepository, never()).findByTontineGestionnaireTelephone(any());
    }

    // ------------------------------------------------------------ suppression

    @Test
    void supprimer_parUnEtranger_estRefuseSansSupprimer() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(41L))
                .thenReturn(Optional.of(cycle(tontine(StatutTontine.ACTIVE), 1, StatutCycle.EN_COURS)));

        assertThrows(AccesRefuseException.class, () -> cycleService.deleteCycle(41L));
        verify(cycleRepository, never()).deleteById(any());
    }
}
