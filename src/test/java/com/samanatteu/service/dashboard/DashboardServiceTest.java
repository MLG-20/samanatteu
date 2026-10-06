package com.samanatteu.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.dto.dashboard.CotisationRetardDTO;
import com.samanatteu.dto.dashboard.GainAVerserDTO;
import com.samanatteu.dto.dashboard.PretEnCoursDTO;
import com.samanatteu.dto.dashboard.TirageAFaireDTO;
import com.samanatteu.dto.dashboard.TontineResumeDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.cotisation.Tirage;
import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.cotisation.StatutTirage;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.enums.tontine.StatutCycle;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.cotisation.TirageRepository;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.repository.tontine.CycleRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Tableau de bord gestionnaire : chaque bloc ne lit que les données du
// gestionnaire connecté (téléphone du token) et calcule ses chiffres.
// Gestionnaire 770000101, tontine 6 « Natt des mamans », membre Fatou Ndiaye.
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String GESTIONNAIRE = "770000101";

    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private CycleRepository cycleRepository;
    @Mock
    private CotisationRepository cotisationRepository;
    @Mock
    private PretRepository pretRepository;
    @Mock
    private EcheancePretRepository echeancePretRepository;
    @Mock
    private TirageRepository tirageRepository;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void connecterLeGestionnaire() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(GESTIONNAIRE, null,
                        List.of(new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"))));
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ aides

    // compareTo et non equals : 400 et 400.00 sont la même somme.
    private void assertMontant(String attendu, BigDecimal reel) {
        assertEquals(0, new BigDecimal(attendu).compareTo(reel),
                "attendu " + attendu + " mais reçu " + reel);
    }

    private Tontine tontine() {
        Tontine t = new Tontine();
        t.setId(6L);
        t.setNom("Natt des mamans");
        t.setStatut(StatutTontine.ACTIVE);
        t.setSoldeCaissePret(new BigDecimal("1200"));
        return t;
    }

    private Cycle cycle(Long id, int numero, StatutCycle statut) {
        Cycle c = new Cycle();
        c.setId(id);
        c.setTontine(tontine());
        c.setNumeroCycle(numero);
        c.setStatut(statut);
        c.setMontantAttendu(new BigDecimal("15000"));
        c.setMontantCollecte(new BigDecimal("7000"));
        c.setDateFinPrevue(LocalDate.of(2026, 11, 6));
        c.setDateFinReelle(LocalDate.of(2026, 10, 6));
        return c;
    }

    private Utilisateur fatou() {
        Utilisateur m = new Utilisateur();
        m.setId(3L);
        m.setPrenom("Fatou");
        m.setNom("Ndiaye");
        m.setTelephone("771110002");
        return m;
    }

    private Participation participationDeFatou() {
        Participation p = new Participation();
        p.setId(10L);
        p.setMembre(fatou());
        p.setTontine(tontine());
        return p;
    }

    private EcheancePret echeance(int numero, String du, String paye, LocalDate date) {
        EcheancePret e = new EcheancePret();
        e.setNumeroEcheance(numero);
        e.setMontantDu(new BigDecimal(du));
        e.setMontantPaye(new BigDecimal(paye));
        e.setDateEcheance(date);
        return e;
    }

    private Pret pret(StatutPret statut) {
        Pret p = new Pret();
        p.setId(2L);
        p.setTontine(tontine());
        p.setMembre(fatou());
        p.setMontant(new BigDecimal("1000"));
        p.setMontantInteret(new BigDecimal("100"));
        p.setStatut(statut);
        return p;
    }

    private Tirage tirage(Long id, StatutTirage statut, String gagne, String verse) {
        Tirage t = new Tirage();
        t.setId(id);
        t.setCycle(cycle(5L, 1, StatutCycle.CLOTURE));
        t.setParticipation(participationDeFatou());
        t.setMontantGagne(new BigDecimal(gagne));
        t.setMontantVerse(new BigDecimal(verse));
        t.setStatut(statut);
        return t;
    }

    // ------------------------------------------------------------ mes tontines

    // Cycle en cours : ses trois chiffres sont recopiés, à côté du nombre de
    // membres ACTIF (compté en base) et du solde de la caisse de prêts.
    @Test
    void mesTontines_avecUnCycleEnCours_remplitLesChiffresDuCycle() {
        when(tontineRepository.findByGestionnaireTelephone(GESTIONNAIRE)).thenReturn(List.of(tontine()));
        when(participationRepository.countByTontineIdAndStatut(6L, StatutParticipation.ACTIF)).thenReturn(3L);
        when(cycleRepository.findByTontineIdAndStatut(6L, StatutCycle.EN_COURS))
                .thenReturn(Optional.of(cycle(5L, 2, StatutCycle.EN_COURS)));

        List<TontineResumeDTO> resultat = dashboardService.mesTontines();

        assertEquals(1, resultat.size());
        TontineResumeDTO ligne = resultat.get(0);
        assertEquals(6L, ligne.getId());
        assertEquals("Natt des mamans", ligne.getNom());
        assertEquals(StatutTontine.ACTIVE, ligne.getStatut());
        assertEquals(3L, ligne.getNombreMembres());
        assertMontant("1200", ligne.getSoldeCaissePret());
        assertEquals(2, ligne.getNumeroCycleEnCours());
        assertMontant("7000", ligne.getMontantCollecte());
        assertMontant("15000", ligne.getMontantAttendu());
        // Les tontines du gestionnaire connecté, jamais toutes les tontines.
        verify(tontineRepository, never()).findAll();
    }

    // Pas de cycle en cours (tontine EN_ATTENTE, ou cycle clôturé) : les trois
    // champs restent null, le front affiche « pas de cycle en cours ». Le
    // reste de la ligne est quand même rempli.
    @Test
    void mesTontines_sansCycleEnCours_laisseLesChiffresDuCycleANull() {
        when(tontineRepository.findByGestionnaireTelephone(GESTIONNAIRE)).thenReturn(List.of(tontine()));
        when(participationRepository.countByTontineIdAndStatut(6L, StatutParticipation.ACTIF)).thenReturn(3L);
        when(cycleRepository.findByTontineIdAndStatut(6L, StatutCycle.EN_COURS)).thenReturn(Optional.empty());

        TontineResumeDTO ligne = dashboardService.mesTontines().get(0);

        assertNull(ligne.getNumeroCycleEnCours());
        assertNull(ligne.getMontantCollecte());
        assertNull(ligne.getMontantAttendu());
        assertEquals(3L, ligne.getNombreMembres());
        assertMontant("1200", ligne.getSoldeCaissePret());
    }

    // ------------------------------------------------- cotisations en retard

    // Reste dû = reste de la PART + reste de la CAISSE de prêts (5 000 - 2 000
    // + 500 - 100 = 3 400), comme le plafond de enregistrerPaiement.
    @Test
    void cotisationRetard_additionneLeResteDeLaPartEtDeLaCaisse() {
        Cotisation cotisation = new Cotisation();
        cotisation.setId(2L);
        cotisation.setCycle(cycle(5L, 1, StatutCycle.CLOTURE));
        cotisation.setParticipation(participationDeFatou());
        cotisation.setMontantDu(new BigDecimal("5000"));
        cotisation.setMontantPaye(new BigDecimal("2000"));
        cotisation.setMontantCaisseDu(new BigDecimal("500"));
        cotisation.setMontantCaissePaye(new BigDecimal("100"));
        cotisation.setStatut(StatutCotisation.EN_RETARD);
        when(cotisationRepository.findByStatutAndCycleTontineGestionnaireTelephone(
                StatutCotisation.EN_RETARD, GESTIONNAIRE)).thenReturn(List.of(cotisation));

        List<CotisationRetardDTO> resultat = dashboardService.cotisationRetard();

        assertEquals(1, resultat.size());
        CotisationRetardDTO ligne = resultat.get(0);
        assertEquals(2L, ligne.getCotisationId());
        assertEquals("Natt des mamans", ligne.getTontineNom());
        assertEquals(1, ligne.getNumeroCycle());
        assertEquals("Fatou Ndiaye", ligne.getMembreNom());
        assertEquals("771110002", ligne.getMembreTelephone());
        assertMontant("3400", ligne.getResteDu());
    }

    // Aucun retard : une liste vide (pas null), le front affiche « rien à relancer ».
    @Test
    void cotisationRetard_sansRetard_rendUneListeVide() {
        when(cotisationRepository.findByStatutAndCycleTontineGestionnaireTelephone(
                StatutCotisation.EN_RETARD, GESTIONNAIRE)).thenReturn(List.of());

        assertTrue(dashboardService.cotisationRetard().isEmpty());
    }

    // --------------------------------------------------------- prêts en cours

    // Le statut affiché est celui DU PRÊT (ici EN_RETARD), jamais une valeur
    // fixe : c'est lui qui signale le retard. Reste = somme des restes des
    // échéances non payées (550 - 150 + 550 = 950) ; prochaine échéance = la
    // première de la liste triée.
    @Test
    void pretEnCours_recopieLeStatutReelEtAdditionneLesEcheancesRestantes() {
        when(pretRepository.findByStatutInAndTontineGestionnaireTelephone(
                List.of(StatutPret.ACTIF, StatutPret.EN_RETARD), GESTIONNAIRE))
                .thenReturn(List.of(pret(StatutPret.EN_RETARD)));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(2L, StatutEcheancePret.PAYE))
                .thenReturn(List.of(
                        echeance(1, "550", "150", LocalDate.of(2026, 9, 6)),
                        echeance(2, "550", "0", LocalDate.of(2026, 10, 6))));

        List<PretEnCoursDTO> resultat = dashboardService.pretEnCours();

        assertEquals(1, resultat.size());
        PretEnCoursDTO ligne = resultat.get(0);
        assertEquals(2L, ligne.getPretId());
        assertEquals("Natt des mamans", ligne.getTontineNom());
        assertEquals("Fatou Ndiaye", ligne.getMembreNom());
        assertEquals("771110002", ligne.getMembreTelephone());
        assertEquals(StatutPret.EN_RETARD, ligne.getStatut());
        assertMontant("1100", ligne.getMontantTotal());
        assertMontant("950", ligne.getResteARembourser());
        assertEquals(LocalDate.of(2026, 9, 6), ligne.getProchaineEcheance());
    }

    // Garde-fou : aucune échéance restante → reste 0 et pas de date, au lieu
    // d'un get(0) qui planterait sur une liste vide.
    @Test
    void pretEnCours_sansEcheanceRestante_donneUnResteNulSansPlanter() {
        when(pretRepository.findByStatutInAndTontineGestionnaireTelephone(
                List.of(StatutPret.ACTIF, StatutPret.EN_RETARD), GESTIONNAIRE))
                .thenReturn(List.of(pret(StatutPret.ACTIF)));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(2L, StatutEcheancePret.PAYE))
                .thenReturn(List.of());

        PretEnCoursDTO ligne = dashboardService.pretEnCours().get(0);

        assertMontant("0", ligne.getResteARembourser());
        assertNull(ligne.getProchaineEcheance());
        assertEquals(StatutPret.ACTIF, ligne.getStatut());
    }

    // -------------------------------------------------------- tirages à faire

    // Trois cycles : en cours (on ne tire qu'après la clôture), clôturé déjà
    // tiré, clôturé sans tirage. Seul le dernier attend un tirage.
    @Test
    void tirageAFaire_neGardeQueLesCyclesCloturesSansTirage() {
        when(cycleRepository.findByTontineGestionnaireTelephone(GESTIONNAIRE)).thenReturn(List.of(
                cycle(7L, 3, StatutCycle.EN_COURS),
                cycle(5L, 1, StatutCycle.CLOTURE),
                cycle(6L, 2, StatutCycle.CLOTURE)));
        when(tirageRepository.existsByCycleId(5L)).thenReturn(true);
        when(tirageRepository.existsByCycleId(6L)).thenReturn(false);

        List<TirageAFaireDTO> resultat = dashboardService.tirageAFaire();

        assertEquals(1, resultat.size());
        TirageAFaireDTO ligne = resultat.get(0);
        assertEquals(6L, ligne.getCycleId());
        assertEquals(2, ligne.getNumeroCycle());
        assertEquals("Natt des mamans", ligne.getTontineNom());
        // Le jour réel de la clôture, pas la date prévue.
        assertEquals(LocalDate.of(2026, 10, 6), ligne.getDateCloture());
        assertMontant("7000", ligne.getMontantCollecte());
        assertMontant("15000", ligne.getMontantAttendu());
    }

    // --------------------------------------------------------- gains à verser

    // VERSE écarté (plus rien à remettre) ; EN_ATTENTE, PARTIEL et REPORTE
    // restent, dans l'ordre reçu.
    @Test
    void gainAVerse_ecarteLesTiragesEntierementVerses() {
        when(tirageRepository.findByCycleTontineGestionnaireTelephone(GESTIONNAIRE)).thenReturn(List.of(
                tirage(1L, StatutTirage.VERSE, "15000", "15000"),
                tirage(2L, StatutTirage.EN_ATTENTE, "15000", "0"),
                tirage(3L, StatutTirage.PARTIEL, "15000", "7000"),
                tirage(4L, StatutTirage.REPORTE, "15000", "7000")));

        List<GainAVerserDTO> resultat = dashboardService.gainAVerse();

        assertEquals(List.of(2L, 3L, 4L), resultat.stream().map(GainAVerserDTO::getTirageId).toList());
    }

    // Reste à verser = gagné - déjà versé (15 000 - 7 000), et le gagnant est
    // le membre de la participation du tirage.
    @Test
    void gainAVerse_calculeLeResteEtNommeLeGagnant() {
        when(tirageRepository.findByCycleTontineGestionnaireTelephone(GESTIONNAIRE))
                .thenReturn(List.of(tirage(3L, StatutTirage.PARTIEL, "15000", "7000")));

        GainAVerserDTO ligne = dashboardService.gainAVerse().get(0);

        assertEquals(3L, ligne.getTirageId());
        assertEquals("Natt des mamans", ligne.getTontineNom());
        assertEquals(1, ligne.getNumeroCycle());
        assertEquals("Fatou Ndiaye", ligne.getGagnantNom());
        assertEquals("771110002", ligne.getGagnantTelephone());
        assertMontant("15000", ligne.getMontantGagne());
        assertMontant("7000", ligne.getMontantVerse());
        assertMontant("8000", ligne.getResteAVerser());
        assertEquals(StatutTirage.PARTIEL, ligne.getStatut());
    }
}
