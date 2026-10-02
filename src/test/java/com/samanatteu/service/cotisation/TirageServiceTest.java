package com.samanatteu.service.cotisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import com.samanatteu.dto.cotisation.TirageDTO;
import com.samanatteu.dto.cotisation.VersementDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.cotisation.Tirage;
import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.cotisation.StatutTirage;
import com.samanatteu.enums.pret.TypeTransaction;
import com.samanatteu.enums.tontine.StatutCycle;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.cotisation.MontantVerseSuperieurAuDisponibleException;
import com.samanatteu.exception.cotisation.TirageDejaExistantPourCeCycleException;
import com.samanatteu.exception.cotisation.TirageIntrouvableException;
import com.samanatteu.exception.cotisation.TirageNonReportableException;
import com.samanatteu.exception.cotisation.UrneVideException;
import com.samanatteu.exception.tontine.CycleIntrouvableException;
import com.samanatteu.exception.tontine.CycleNonClotureException;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.cotisation.TirageRepository;
import com.samanatteu.repository.tontine.CycleRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.security.UtilisateurConnecte;
import com.samanatteu.service.pret.TransactionService;
import com.samanatteu.service.notification.NotificationService;
import com.samanatteu.enums.notification.TypeNotification;

// Tests des règles de TirageService (tirage au sort, compensation, versement,
// report, lecture filtrée), avec de faux repositories.
// Convention : la tontine 6 appartient au gestionnaire 770000101 ; un
// "étranger" est 770000102. Le cycle 5 a une cagnotte attendue de 40 000 F.
// Membres : Awa (participation 1), Binta (2), Coumba (3).
@ExtendWith(MockitoExtension.class)
class TirageServiceTest {

    @Mock
    private TirageRepository tirageRepository;
    @Mock
    private CycleRepository cycleRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private CotisationRepository cotisationRepository;
    // Faux journal : on vérifie seulement QUE et COMMENT il est appelé.
    @Mock
    private TransactionService transactionService;
    // Un vrai objet : il lit le SecurityContextHolder rempli par connecter().
    // Les envois (SMS/email) sont vérifiés par NotificationServiceTest ; ici on
    // vérifie seulement que le service métier les DÉCLENCHE.
    @Mock
    private NotificationService notificationService;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private TirageService tirageService;

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

    private Tontine tontine() {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setTelephone("770000101");
        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setGestionnaire(gestionnaire);
        return tontine;
    }

    // Cycle 5 de la tontine 6 : 40 000 attendus, "collecte" déjà en caisse.
    private Cycle cycle(StatutCycle statut, String collecte) {
        Cycle cycle = new Cycle();
        cycle.setId(5L);
        cycle.setTontine(tontine());
        cycle.setStatut(statut);
        cycle.setMontantAttendu(new BigDecimal("40000"));
        cycle.setMontantCollecte(new BigDecimal(collecte));
        return cycle;
    }

    private Participation membre(long id, int parts) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(100 + id);
        Participation p = new Participation();
        p.setId(id);
        p.setMembre(utilisateur);
        p.setNombreParts(parts);
        p.setStatut(StatutParticipation.ACTIF);
        return p;
    }

    // Un tirage déjà fait sur le cycle 5 : 40 000 gagnés, "verse" déjà remis.
    private Tirage tirage(StatutTirage statut, String verse, String collecteDuCycle) {
        Tirage tirage = new Tirage();
        tirage.setId(7L);
        tirage.setCycle(cycle(StatutCycle.CLOTURE, collecteDuCycle));
        tirage.setParticipation(membre(1L, 1));
        tirage.setMontantGagne(new BigDecimal("40000"));
        tirage.setMontantVerse(new BigDecimal(verse));
        tirage.setStatut(statut);
        return tirage;
    }

    private VersementDTO versement(String montant) {
        VersementDTO dto = new VersementDTO();
        dto.setMontant(new BigDecimal(montant));
        return dto;
    }

    // Le cycle 5 est CLOTURE, pas encore tiré, et ces membres sont ACTIF.
    private void cycleClotureAvecMembres(Cycle cycle, Participation... membres) {
        when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle));
        when(tirageRepository.existsByCycleId(5L)).thenReturn(false);
        when(participationRepository.findByTontineIdAndStatut(6L, StatutParticipation.ACTIF))
                .thenReturn(List.of(membres));
    }

    // Le faux save() renvoie l'objet reçu (comme la vraie base).
    private void saveDuTirageRenvoieLeTirage() {
        when(tirageRepository.save(any(Tirage.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    // Comme saveDuTirageRenvoieLeTirage, mais la "base" donne l'id 99 au
    // tirage : prouve que le journal reçoit l'id APRÈS le save.
    private void saveDuTirageDonneLId99() {
        when(tirageRepository.save(any(Tirage.class))).thenAnswer(appel -> {
            Tirage t = appel.getArgument(0);
            t.setId(99L);
            return t;
        });
    }

    // Une cotisation EN_RETARD (id 70) : part 10 000 dont "paye" versés,
    // caisse de prêts "caisseDu" dont rien versé.
    private Cotisation dette(String paye, String caisseDu) {
        Cotisation dette = new Cotisation();
        dette.setId(70L);
        dette.setMontantDu(new BigDecimal("10000"));
        dette.setMontantPaye(new BigDecimal(paye));
        dette.setMontantCaisseDu(new BigDecimal(caisseDu));
        dette.setMontantCaissePaye(BigDecimal.ZERO);
        dette.setStatut(StatutCotisation.EN_RETARD);
        return dette;
    }

    // Faux hasard : renvoie toujours l'indice voulu et retient la taille de
    // l'urne reçue (bound), pour vérifier le nombre de membres en lice.
    private static class HasardTruque extends Random {
        private final int indice;
        int tailleUrne;

        HasardTruque(int indice) {
            this.indice = indice;
        }

        @Override
        public int nextInt(int bound) {
            tailleUrne = bound;
            return indice;
        }
    }

    // ------------------------------------------------------ tirage : refus

    @Test
    void tirer_cycleInexistant_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(CycleIntrouvableException.class, () -> tirageService.tirerAuSort(5L));
    }

    // Droit avant faisabilité : l'étranger est refusé avant tout examen du cycle.
    @Test
    void tirer_parUnEtranger_donne403() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle(StatutCycle.EN_COURS, "0")));

        assertThrows(AccesRefuseException.class, () -> tirageService.tirerAuSort(5L));
        verify(tirageRepository, never()).save(any());
    }

    // Décision : on ne tire qu'après la clôture (on collecte tout, puis on tire).
    @Test
    void tirer_cycleEnCours_donne409() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle(StatutCycle.EN_COURS, "0")));

        assertThrows(CycleNonClotureException.class, () -> tirageService.tirerAuSort(5L));
        verify(tirageRepository, never()).save(any());
    }

    // CDC : un cycle a exactement un tirage.
    @Test
    void tirer_cycleDejaTire_donne409() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle(StatutCycle.CLOTURE, "40000")));
        when(tirageRepository.existsByCycleId(5L)).thenReturn(true);

        assertThrows(TirageDejaExistantPourCeCycleException.class, () -> tirageService.tirerAuSort(5L));
        verify(tirageRepository, never()).save(any());
    }

    // Tout le monde a déjà gagné autant de fois qu'il a de parts : 409, pas de 500.
    @Test
    void tirer_urneVide_donne409() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "40000"), membre(1L, 2), membre(2L, 1));
        when(tirageRepository.countByParticipationId(1L)).thenReturn(2L);
        when(tirageRepository.countByParticipationId(2L)).thenReturn(1L);

        assertThrows(UrneVideException.class, () -> tirageService.tirerAuSort(5L));
        verify(tirageRepository, never()).save(any());
    }

    // --------------------------------------------------------- tirage : urne

    // Chance égale : Awa (2 parts), Binta (1), Coumba (1), personne n'a gagné.
    // L'urne doit contenir 3 membres (1 chance sur 3 chacun), pas 4 tickets.
    @Test
    void tirer_chaqueMembreEnLiceFigureUneSeuleFoisDansLUrne() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "40000"),
                membre(1L, 2), membre(2L, 1), membre(3L, 1));
        when(tirageRepository.countByParticipationId(anyLong())).thenReturn(0L);
        saveDuTirageRenvoieLeTirage();
        HasardTruque hasard = new HasardTruque(0);
        ReflectionTestUtils.setField(tirageService, "hasard", hasard);

        tirageService.tirerAuSort(5L);

        assertEquals(3, hasard.tailleUrne);
    }

    // Modèle B : Binta (1 part) a déjà gagné une fois, elle sort de l'urne ;
    // Awa (2 parts, 1 gain) y reste. Seuls Awa et Coumba sont en lice.
    @Test
    void tirer_unMembreQuiAEpuiseSesPartsNEstPlusDansLUrne() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "40000"),
                membre(1L, 2), membre(2L, 1), membre(3L, 1));
        when(tirageRepository.countByParticipationId(1L)).thenReturn(1L);
        when(tirageRepository.countByParticipationId(2L)).thenReturn(1L);
        when(tirageRepository.countByParticipationId(3L)).thenReturn(0L);
        saveDuTirageRenvoieLeTirage();
        // indice 1 = le 2e membre en lice : Coumba (Binta a été écartée).
        HasardTruque hasard = new HasardTruque(1);
        ReflectionTestUtils.setField(tirageService, "hasard", hasard);

        TirageDTO resultat = tirageService.tirerAuSort(5L);

        assertEquals(2, hasard.tailleUrne);
        assertEquals(3L, resultat.getParticipationId());
    }

    // ----------------------------------------------- tirage : montants/statut

    // Caisse pleine : le gagnant reçoit tout de suite ses 40 000 → VERSE.
    @Test
    void tirer_caissePleine_donneVerse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "40000"), membre(1L, 1));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.tirerAuSort(5L);

        assertEquals(1L, resultat.getParticipationId());
        assertEquals(5L, resultat.getCycleId());
        assertEquals(0, new BigDecimal("40000").compareTo(resultat.getMontantGagne()));
        assertEquals(0, new BigDecimal("40000").compareTo(resultat.getMontantVerse()));
        assertEquals(StatutTirage.VERSE, resultat.getStatut());
        assertNotNull(resultat.getDateTirage());
        assertNotNull(resultat.getDateVersement());
    }

    // Un autre membre est en retard : le gagnant gagne 40 000 mais n'en
    // reçoit que 30 000 (la caisse) → PARTIEL, le reste viendra via verser().
    @Test
    void tirer_caissePartielle_donnePartiel() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "30000"), membre(1L, 1));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.tirerAuSort(5L);

        assertEquals(0, new BigDecimal("40000").compareTo(resultat.getMontantGagne()));
        assertEquals(0, new BigDecimal("30000").compareTo(resultat.getMontantVerse()));
        assertEquals(StatutTirage.PARTIEL, resultat.getStatut());
    }

    // Personne n'a payé : rien remis → EN_ATTENTE, et pas de date de versement.
    @Test
    void tirer_caisseVide_donneEnAttenteSansDateDeVersement() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "0"), membre(1L, 1));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.tirerAuSort(5L);

        assertEquals(StatutTirage.EN_ATTENTE, resultat.getStatut());
        assertNull(resultat.getDateVersement());
    }

    // ------------------------------------------------ tirage : compensation

    // Coumba gagne alors qu'elle est EN_RETARD (4 000 payés sur 10 000) :
    // les 6 000 manquants sont payés avec son gain. Cotisation COMPLET,
    // caisse 34 000 + 6 000 = 40 000, tirage soldé (VERSE).
    @Test
    void tirer_gagnantEnRetard_compenseSaDette() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cycle cycle = cycle(StatutCycle.CLOTURE, "34000");
        cycleClotureAvecMembres(cycle, membre(3L, 1));
        Cotisation dette = new Cotisation();
        dette.setMontantDu(new BigDecimal("10000"));
        dette.setMontantPaye(new BigDecimal("4000"));
        dette.setMontantCaisseDu(BigDecimal.ZERO);
        dette.setMontantCaissePaye(BigDecimal.ZERO);
        dette.setStatut(StatutCotisation.EN_RETARD);
        when(cotisationRepository.findByCycleIdAndParticipationId(5L, 3L)).thenReturn(Optional.of(dette));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.tirerAuSort(5L);

        assertEquals(StatutCotisation.COMPLET, dette.getStatut());
        assertEquals(0, new BigDecimal("10000").compareTo(dette.getMontantPaye()));
        verify(cotisationRepository).save(dette);
        assertEquals(0, new BigDecimal("40000").compareTo(cycle.getMontantCollecte()));
        verify(cycleRepository).save(cycle);
        assertEquals(0, new BigDecimal("40000").compareTo(resultat.getMontantVerse()));
        assertEquals(StatutTirage.VERSE, resultat.getStatut());
    }

    // Même cas, mais il doit aussi 500 de caisse de prêts : le gain efface
    // la part, jamais la caisse → il reste EN_RETARD, caisse toujours due.
    @Test
    void tirer_gagnantEnRetardAvecCaisseImpayee_resteEnRetard() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cycle cycle = cycle(StatutCycle.CLOTURE, "34000");
        cycleClotureAvecMembres(cycle, membre(3L, 1));
        Cotisation dette = new Cotisation();
        dette.setMontantDu(new BigDecimal("10000"));
        dette.setMontantPaye(new BigDecimal("4000"));
        dette.setMontantCaisseDu(new BigDecimal("500"));
        dette.setMontantCaissePaye(BigDecimal.ZERO);
        dette.setStatut(StatutCotisation.EN_RETARD);
        when(cotisationRepository.findByCycleIdAndParticipationId(5L, 3L)).thenReturn(Optional.of(dette));
        saveDuTirageRenvoieLeTirage();

        tirageService.tirerAuSort(5L);

        assertEquals(StatutCotisation.EN_RETARD, dette.getStatut());
        assertEquals(0, new BigDecimal("10000").compareTo(dette.getMontantPaye()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dette.getMontantCaissePaye()));
        assertEquals(0, new BigDecimal("40000").compareTo(cycle.getMontantCollecte()));
    }

    // Le gagnant est à jour (COMPLET) : aucune compensation, rien à toucher.
    @Test
    void tirer_gagnantAJour_pasDeCompensation() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "30000"), membre(1L, 1));
        Cotisation aJour = new Cotisation();
        aJour.setStatut(StatutCotisation.COMPLET);
        when(cotisationRepository.findByCycleIdAndParticipationId(5L, 1L)).thenReturn(Optional.of(aJour));
        saveDuTirageRenvoieLeTirage();

        tirageService.tirerAuSort(5L);

        verify(cotisationRepository, never()).save(any());
        verify(cycleRepository, never()).save(any());
    }

    // ------------------------------------------------------------- verser

    @Test
    void verser_tirageInexistant_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(TirageIntrouvableException.class,
                () -> tirageService.verser(7L, versement("10000")));
    }

    @Test
    void verser_parUnEtranger_donne403() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.PARTIEL, "30000", "40000")));

        assertThrows(AccesRefuseException.class,
                () -> tirageService.verser(7L, versement("10000")));
        verify(tirageRepository, never()).save(any());
    }

    // Caisse 40 000, déjà remis 30 000 : disponible 10 000. Verser 15 000 = 400.
    @Test
    void verser_plusQueLeDisponible_donne400() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.PARTIEL, "30000", "40000")));

        assertThrows(MontantVerseSuperieurAuDisponibleException.class,
                () -> tirageService.verser(7L, versement("15000")));
        verify(tirageRepository, never()).save(any());
    }

    // Tirage déjà VERSE : disponible 0, tout versement est refusé.
    @Test
    void verser_surUnTirageDejaVerse_donne400() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.VERSE, "40000", "40000")));

        assertThrows(MontantVerseSuperieurAuDisponibleException.class,
                () -> tirageService.verser(7L, versement("1")));
    }

    // Verser exactement le disponible solde le tirage → VERSE.
    @Test
    void verser_leResteComplet_donneVerse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.PARTIEL, "30000", "40000")));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.verser(7L, versement("10000"));

        assertEquals(0, new BigDecimal("40000").compareTo(resultat.getMontantVerse()));
        assertEquals(StatutTirage.VERSE, resultat.getStatut());
        assertNotNull(resultat.getDateVersement());
    }

    // Une partie seulement du reste → PARTIEL.
    @Test
    void verser_unePartieDuReste_donnePartiel() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.EN_ATTENTE, "0", "30000")));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.verser(7L, versement("20000"));

        assertEquals(0, new BigDecimal("20000").compareTo(resultat.getMontantVerse()));
        assertEquals(StatutTirage.PARTIEL, resultat.getStatut());
    }

    // Fin d'un report : verser fait sortir le tirage de REPORTE.
    @Test
    void verser_surUnTirageReporte_leFaitRepartir() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.REPORTE, "0", "40000")));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.verser(7L, versement("40000"));

        assertEquals(StatutTirage.VERSE, resultat.getStatut());
    }

    // ------------------------------------------------------------ journal

    // Caisse pleine : une ligne GAIN de 40 000 pour le gagnant, avec l'id du
    // tirage enregistré ; pas de ligne de compensation.
    @Test
    void tirer_caissePleine_journaliseUnGainAvecLIdDuTirage() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cycle cycle = cycle(StatutCycle.CLOTURE, "40000");
        Participation awa = membre(1L, 1);
        cycleClotureAvecMembres(cycle, awa);
        saveDuTirageDonneLId99();

        tirageService.tirerAuSort(5L);

        verify(transactionService).journaliser(eq(awa.getMembre()), eq(cycle.getTontine()),
                eq(TypeTransaction.GAIN), eq(new BigDecimal("40000")), isNull(), isNull(), eq(99L), isNull());
        verify(transactionService, never()).journaliser(any(), any(), eq(TypeTransaction.COTISATION),
                any(), any(), any(), any(), any());
    }

    // Option A : la dette de Coumba (6 000) réglée par son gain s'écrit comme
    // une COTISATION sans mode, puis le GAIN complet de 40 000.
    @Test
    void tirer_gagnantEnRetard_journaliseLaCompensationPuisLeGain() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Cycle cycle = cycle(StatutCycle.CLOTURE, "34000");
        Participation coumba = membre(3L, 1);
        cycleClotureAvecMembres(cycle, coumba);
        when(cotisationRepository.findByCycleIdAndParticipationId(5L, 3L))
                .thenReturn(Optional.of(dette("4000", "0")));
        saveDuTirageDonneLId99();

        tirageService.tirerAuSort(5L);

        verify(transactionService).journaliser(eq(coumba.getMembre()), eq(cycle.getTontine()),
                eq(TypeTransaction.COTISATION), eq(new BigDecimal("6000")), isNull(), isNull(), eq(70L),
                eq("Compensation par le gain du tirage"));
        verify(transactionService).journaliser(eq(coumba.getMembre()), eq(cycle.getTontine()),
                eq(TypeTransaction.GAIN), eq(new BigDecimal("40000")), isNull(), isNull(), eq(99L), isNull());
    }

    // Part entièrement payée, seule la caisse manque : resteDu = 0, donc pas
    // de ligne de compensation (CHECK montant > 0 annulerait tout le tirage).
    @Test
    void tirer_gagnantEnRetardSeulementSurLaCaisse_pasDeLigneDeCompensation() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "40000"), membre(3L, 1));
        when(cotisationRepository.findByCycleIdAndParticipationId(5L, 3L))
                .thenReturn(Optional.of(dette("10000", "500")));
        saveDuTirageDonneLId99();

        tirageService.tirerAuSort(5L);

        verify(transactionService, never()).journaliser(any(), any(), eq(TypeTransaction.COTISATION),
                any(), any(), any(), any(), any());
    }

    // Caisse vide (EN_ATTENTE) : rien n'est remis, donc aucune ligne à 0.
    @Test
    void tirer_caisseVide_nEcritRienAuJournal() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        cycleClotureAvecMembres(cycle(StatutCycle.CLOTURE, "0"), membre(1L, 1));
        saveDuTirageRenvoieLeTirage();

        tirageService.tirerAuSort(5L);

        verifyNoInteractions(transactionService);
    }

    // verser : une ligne GAIN du montant remis MAINTENANT (pas du total).
    @Test
    void verser_journaliseUnGainDuMontantRemis() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tirage tirage = tirage(StatutTirage.PARTIEL, "30000", "40000");
        when(tirageRepository.findById(7L)).thenReturn(Optional.of(tirage));
        saveDuTirageRenvoieLeTirage();

        tirageService.verser(7L, versement("10000"));

        verify(transactionService).journaliser(eq(tirage.getParticipation().getMembre()),
                eq(tirage.getCycle().getTontine()), eq(TypeTransaction.GAIN), eq(new BigDecimal("10000")),
                isNull(), isNull(), eq(7L), isNull());
    }

    @Test
    void verser_refuse_nEcritRienAuJournal() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.PARTIEL, "30000", "40000")));

        assertThrows(MontantVerseSuperieurAuDisponibleException.class,
                () -> tirageService.verser(7L, versement("15000")));
        verifyNoInteractions(transactionService);
    }

    // ------------------------------------------------------------ reporter

    // Il reste de l'argent à remettre : on peut reporter.
    @ParameterizedTest
    @EnumSource(value = StatutTirage.class, names = { "EN_ATTENTE", "PARTIEL" })
    void reporter_unTirageNonSolde_passeEnReporte(StatutTirage statut) {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L)).thenReturn(Optional.of(tirage(statut, "0", "40000")));
        saveDuTirageRenvoieLeTirage();

        TirageDTO resultat = tirageService.reporter(7L);

        assertEquals(StatutTirage.REPORTE, resultat.getStatut());
    }

    // VERSE : plus rien à reporter ; REPORTE : déjà fait.
    @ParameterizedTest
    @EnumSource(value = StatutTirage.class, names = { "VERSE", "REPORTE" })
    void reporter_unTirageSoldeOuDejaReporte_donne409(StatutTirage statut) {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L)).thenReturn(Optional.of(tirage(statut, "0", "40000")));

        assertThrows(TirageNonReportableException.class, () -> tirageService.reporter(7L));
        verify(tirageRepository, never()).save(any());
    }

    @Test
    void reporter_parUnEtranger_donne403() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L))
                .thenReturn(Optional.of(tirage(StatutTirage.EN_ATTENTE, "0", "40000")));

        assertThrows(AccesRefuseException.class, () -> tirageService.reporter(7L));
        verify(tirageRepository, never()).save(any());
    }

    @Test
    void reporter_tirageInexistant_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(TirageIntrouvableException.class, () -> tirageService.reporter(7L));
    }

    // ------------------------------------------------------------ lecture

    // Le gestionnaire ne voit que les tirages de SES tontines, jamais findAll.
    @Test
    void lister_gestionnaire_voitLesTiragesDeSesTontines() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tirageRepository.findByCycleTontineGestionnaireTelephone("770000101"))
                .thenReturn(List.of(tirage(StatutTirage.VERSE, "40000", "40000")));

        List<TirageDTO> resultat = tirageService.listTirage();

        assertEquals(1, resultat.size());
        verify(tirageRepository, never()).findAll();
    }

    // Le membre voit tous les tirages des tontines où il participe (US-M03).
    @Test
    void lister_membre_voitLesTiragesDeSesTontines() {
        connecter("771234566", "ROLE_MEMBRE");
        Participation saParticipation = membre(3L, 1);
        saParticipation.setTontine(tontine());
        when(participationRepository.findByMembreTelephone("771234566"))
                .thenReturn(List.of(saParticipation));
        when(tirageRepository.findByCycleTontineIdIn(List.of(6L)))
                .thenReturn(List.of(tirage(StatutTirage.VERSE, "40000", "40000")));

        List<TirageDTO> resultat = tirageService.listTirage();

        assertEquals(1, resultat.size());
        verify(tirageRepository, never()).findByCycleTontineGestionnaireTelephone(any());
    }

    // ---------------------------------------------------------- notifications

    // Le gagnant est prévenu : montant gagné ET déjà remis.
    @Test
    void tirer_previentLeGagnant() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Participation gagnant = membre(1L, 1);
        Cycle cycle = cycle(StatutCycle.CLOTURE, "30000");
        cycle.getTontine().setNom("Natt des femmes");
        cycleClotureAvecMembres(cycle, gagnant);
        saveDuTirageRenvoieLeTirage();

        tirageService.tirerAuSort(5L);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifier(eq(gagnant.getMembre()), eq(TypeNotification.RESULTAT_TIRAGE),
                message.capture());
        assertTrue(message.getValue().startsWith("Natt des femmes : félicitations"));
        assertTrue(message.getValue().contains("40000 F"));
        assertTrue(message.getValue().contains("Déjà remis : 30000 F"));
    }

    @Test
    void tirer_refuse_nEnvoieAucuneNotification() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(cycleRepository.findById(5L)).thenReturn(Optional.of(cycle(StatutCycle.EN_COURS, "0")));

        assertThrows(CycleNonClotureException.class, () -> tirageService.tirerAuSort(5L));
        verifyNoInteractions(notificationService);
    }
}
