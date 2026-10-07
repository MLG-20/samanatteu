package com.samanatteu.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

import com.samanatteu.dto.dashboard.CotisationAPayerDTO;
import com.samanatteu.dto.dashboard.MaTontineDTO;
import com.samanatteu.dto.dashboard.MonGainDTO;
import com.samanatteu.dto.dashboard.MonPretDTO;
import com.samanatteu.entity.cotisation.Cotisation;
import com.samanatteu.entity.cotisation.Tirage;
import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.entity.tontine.Cycle;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.enums.cotisation.StatutCotisation;
import com.samanatteu.enums.cotisation.StatutTirage;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.repository.cotisation.CotisationRepository;
import com.samanatteu.repository.cotisation.TirageRepository;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Tableau de bord membre : chaque bloc ne lit que les données du membre
// connecté (téléphone du token), jamais celles des autres ni par une requête
// de gestionnaire. Membre 771110002, tontine 6 « Natt des mamans » (part
// 5 000, caisse 500).
@ExtendWith(MockitoExtension.class)
class DashboardMembreServiceTest {

    private static final String MEMBRE = "771110002";

    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private TirageRepository tirageRepository;
    @Mock
    private CotisationRepository cotisationRepository;
    @Mock
    private PretRepository pretRepository;
    @Mock
    private EcheancePretRepository echeancePretRepository;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private DashboardMembreService dashboardMembreService;

    @BeforeEach
    void connecterLeMembre() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(MEMBRE, null,
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"))));
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
        t.setMontantPart(new BigDecimal("5000"));
        t.setMontantCaissePret(new BigDecimal("500"));
        return t;
    }

    private Participation participation(int nombreParts) {
        Participation p = new Participation();
        p.setId(10L);
        p.setTontine(tontine());
        p.setNombreParts(nombreParts);
        return p;
    }

    private Cycle cycle(int numero) {
        Cycle c = new Cycle();
        c.setId(5L);
        c.setTontine(tontine());
        c.setNumeroCycle(numero);
        c.setDateFinPrevue(LocalDate.of(2026, 11, 6));
        return c;
    }

    private Cotisation cotisation(Long id, StatutCotisation statut, String partPayee, String caissePayee) {
        Cotisation c = new Cotisation();
        c.setId(id);
        c.setCycle(cycle(2));
        c.setParticipation(participation(1));
        c.setMontantDu(new BigDecimal("5000"));
        c.setMontantPaye(new BigDecimal(partPayee));
        c.setMontantCaisseDu(new BigDecimal("500"));
        c.setMontantCaissePaye(new BigDecimal(caissePayee));
        c.setStatut(statut);
        return c;
    }

    private Pret pret(Long id, StatutPret statut) {
        Pret p = new Pret();
        p.setId(id);
        p.setTontine(tontine());
        p.setMontant(new BigDecimal("1000"));
        p.setMontantInteret(new BigDecimal("100"));
        p.setStatut(statut);
        return p;
    }

    private EcheancePret echeance(String du, String paye, LocalDate date) {
        EcheancePret e = new EcheancePret();
        e.setMontantDu(new BigDecimal(du));
        e.setMontantPaye(new BigDecimal(paye));
        e.setDateEcheance(date);
        return e;
    }

    private Tirage tirage(Long id, StatutTirage statut, String gagne, String verse) {
        Tirage t = new Tirage();
        t.setId(id);
        t.setCycle(cycle(1));
        t.setParticipation(participation(1));
        t.setMontantGagne(new BigDecimal(gagne));
        t.setMontantVerse(new BigDecimal(verse));
        t.setStatut(statut);
        return t;
    }

    // ------------------------------------------------------------ mes tontines

    // Ce qu'il paie par cycle = parts × part + caisse, la caisse n'étant PAS
    // multipliée par les parts (2 × 5 000 + 500 = 10 500, pas 11 000). Ses
    // gains = les tirages de SA participation.
    @Test
    void maTontine_calculeLeMontantParCycleEtCompteSesGains() {
        when(participationRepository.findByMembreTelephone(MEMBRE)).thenReturn(List.of(participation(2)));
        when(tirageRepository.countByParticipationId(10L)).thenReturn(1L);

        List<MaTontineDTO> resultat = dashboardMembreService.maTontine();

        assertEquals(1, resultat.size());
        MaTontineDTO ligne = resultat.get(0);
        assertEquals(6L, ligne.getTontineId());
        assertEquals("Natt des mamans", ligne.getTontineNom());
        assertEquals(StatutTontine.ACTIVE, ligne.getStatut());
        assertEquals(2, ligne.getNombreParts());
        assertMontant("10500", ligne.getMontantParCycle());
        assertEquals(1L, ligne.getNombreGains());
        // Ses participations à lui, jamais toutes les participations.
        verify(participationRepository, never()).findAll();
    }

    // ---------------------------------------------------------------- à payer

    // COMPLET écarté (rien à payer) ; EN_ATTENTE, PARTIEL et EN_RETARD
    // restent, dans l'ordre reçu.
    @Test
    void cotisationAPayer_ecarteLesCotisationsSoldees() {
        when(cotisationRepository.findByParticipationMembreTelephone(MEMBRE)).thenReturn(List.of(
                cotisation(1L, StatutCotisation.COMPLET, "5000", "500"),
                cotisation(2L, StatutCotisation.EN_ATTENTE, "0", "0"),
                cotisation(3L, StatutCotisation.PARTIEL, "2000", "0"),
                cotisation(4L, StatutCotisation.EN_RETARD, "0", "0")));

        List<CotisationAPayerDTO> resultat = dashboardMembreService.cotisationAPayer();

        assertEquals(List.of(2L, 3L, 4L), resultat.stream().map(CotisationAPayerDTO::getCotisationId).toList());
    }

    // Reste dû = reste de la part + reste de la caisse (5 000 - 2 000 + 500 -
    // 100 = 3 400) ; date limite = fin PRÉVUE du cycle.
    @Test
    void cotisationAPayer_calculeLeResteDuEtDonneLaDateLimite() {
        when(cotisationRepository.findByParticipationMembreTelephone(MEMBRE))
                .thenReturn(List.of(cotisation(3L, StatutCotisation.PARTIEL, "2000", "100")));

        CotisationAPayerDTO ligne = dashboardMembreService.cotisationAPayer().get(0);

        assertEquals(3L, ligne.getCotisationId());
        assertEquals("Natt des mamans", ligne.getTontineNom());
        assertEquals(2, ligne.getNumeroCycle());
        assertMontant("3400", ligne.getResteDu());
        assertEquals(LocalDate.of(2026, 11, 6), ligne.getDateLimite());
        assertEquals(StatutCotisation.PARTIEL, ligne.getStatut());
        // La requête du MEMBRE, pas celle qui passe par le gestionnaire.
        verify(cotisationRepository, never()).findByCycleTontineGestionnaireTelephone(any());
    }

    // -------------------------------------------------------------- mes prêts

    // REMBOURSE écarté ; ACTIF et EN_RETARD restent, avec leur vrai statut.
    @Test
    void monPret_ecarteLesPretsRembourses() {
        when(pretRepository.findByMembreTelephone(MEMBRE)).thenReturn(List.of(
                pret(1L, StatutPret.REMBOURSE),
                pret(2L, StatutPret.ACTIF),
                pret(3L, StatutPret.EN_RETARD)));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(any(), any()))
                .thenReturn(List.of());

        List<MonPretDTO> resultat = dashboardMembreService.monPret();

        assertEquals(List.of(2L, 3L), resultat.stream().map(MonPretDTO::getPretId).toList());
        assertEquals(List.of(StatutPret.ACTIF, StatutPret.EN_RETARD),
                resultat.stream().map(MonPretDTO::getStatut).toList());
    }

    // Total = capital + intérêt (1 000 + 100). Reste = somme des restes des
    // échéances non payées (550 - 150 + 550 = 950) ; prochaine échéance = la
    // première de la liste triée.
    @Test
    void monPret_ajouteLInteretEtAdditionneLesEcheancesRestantes() {
        when(pretRepository.findByMembreTelephone(MEMBRE)).thenReturn(List.of(pret(2L, StatutPret.EN_RETARD)));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(2L, StatutEcheancePret.PAYE))
                .thenReturn(List.of(
                        echeance("550", "150", LocalDate.of(2026, 9, 6)),
                        echeance("550", "0", LocalDate.of(2026, 10, 6))));

        MonPretDTO ligne = dashboardMembreService.monPret().get(0);

        assertEquals("Natt des mamans", ligne.getTontineNom());
        assertMontant("1100", ligne.getMontantTotal());
        assertMontant("950", ligne.getResteARembourser());
        assertEquals(LocalDate.of(2026, 9, 6), ligne.getProchaineEcheance());
        assertEquals(StatutPret.EN_RETARD, ligne.getStatut());
    }

    // Garde-fou : aucune échéance restante → reste 0 et pas de date, au lieu
    // d'un get(0) qui planterait sur une liste vide.
    @Test
    void monPret_sansEcheanceRestante_donneUnResteNulSansPlanter() {
        when(pretRepository.findByMembreTelephone(MEMBRE)).thenReturn(List.of(pret(2L, StatutPret.ACTIF)));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(2L, StatutEcheancePret.PAYE))
                .thenReturn(List.of());

        MonPretDTO ligne = dashboardMembreService.monPret().get(0);

        assertMontant("0", ligne.getResteARembourser());
        assertNull(ligne.getProchaineEcheance());
    }

    // -------------------------------------------------------------- mes gains

    // Les tirages GAGNÉS par lui : la requête passe par la participation. La
    // requête du gestionnaire rendrait une liste vide sans aucune erreur (un
    // membre ne gère aucune tontine) : elle ne doit jamais être appelée ici.
    @Test
    void monGain_litSesTiragesParLaParticipationEtJamaisParLeGestionnaire() {
        when(tirageRepository.findByParticipationMembreTelephone(MEMBRE))
                .thenReturn(List.of(tirage(3L, StatutTirage.PARTIEL, "15000", "7000")));

        List<MonGainDTO> resultat = dashboardMembreService.monGain();

        assertEquals(1, resultat.size());
        MonGainDTO ligne = resultat.get(0);
        assertEquals(3L, ligne.getTirageId());
        assertEquals("Natt des mamans", ligne.getTontineNom());
        assertEquals(1, ligne.getNumeroCycle());
        assertMontant("15000", ligne.getMontantGagne());
        assertMontant("7000", ligne.getMontantVerse());
        assertMontant("8000", ligne.getResteARecevoir());
        assertEquals(StatutTirage.PARTIEL, ligne.getStatut());
        verify(tirageRepository, never()).findByCycleTontineGestionnaireTelephone(any());
    }

    // Pas de filtre : un gain entièrement versé reste affiché (historique de
    // ce qu'il a touché), avec un reste à recevoir de 0.
    @Test
    void monGain_gardeLesGainsEntierementVerses() {
        when(tirageRepository.findByParticipationMembreTelephone(MEMBRE)).thenReturn(List.of(
                tirage(1L, StatutTirage.VERSE, "15000", "15000"),
                tirage(2L, StatutTirage.EN_ATTENTE, "15000", "0")));

        List<MonGainDTO> resultat = dashboardMembreService.monGain();

        assertEquals(List.of(1L, 2L), resultat.stream().map(MonGainDTO::getTirageId).toList());
        assertMontant("0", resultat.get(0).getResteARecevoir());
        assertMontant("15000", resultat.get(1).getResteARecevoir());
    }
}
