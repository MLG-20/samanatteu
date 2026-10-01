package com.samanatteu.service.pret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.samanatteu.dto.cotisation.VersementDTO;
import com.samanatteu.dto.pret.DemandePretDTO;
import com.samanatteu.dto.pret.PretDTO;
import com.samanatteu.entity.pret.EcheancePret;
import com.samanatteu.entity.pret.Pret;
import com.samanatteu.entity.tontine.Participation;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.pret.StatutEcheancePret;
import com.samanatteu.enums.pret.StatutPret;
import com.samanatteu.enums.tontine.FrequenceTontine;
import com.samanatteu.enums.tontine.StatutParticipation;
import com.samanatteu.enums.tontine.StatutTontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.cotisation.MontantPayeSuperieurAuDuException;
import com.samanatteu.exception.pret.CaisseInsuffisanteException;
import com.samanatteu.exception.pret.MembreNonParticipantException;
import com.samanatteu.exception.pret.PretDejaEnCoursException;
import com.samanatteu.exception.pret.PretDejaRembourseException;
import com.samanatteu.exception.pret.PretIntrouvableException;
import com.samanatteu.exception.tontine.TontineIntrouvableException;
import com.samanatteu.exception.tontine.TontineNonActiveException;
import com.samanatteu.repository.pret.EcheancePretRepository;
import com.samanatteu.repository.pret.PretRepository;
import com.samanatteu.repository.tontine.ParticipationRepository;
import com.samanatteu.repository.tontine.TontineRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Prêts : accorder (contrôles, intérêt, échéancier, débit de la caisse),
// rembourser (répartition sur les échéances les plus anciennes, caisse,
// statuts) et lecture filtrée. Tontine 6 gérée par 770000101 ; 770000102
// est un autre gestionnaire. Membre id 3, prêt id 20.
@ExtendWith(MockitoExtension.class)
class PretServiceTest {

    @Mock
    private PretRepository pretRepository;
    @Mock
    private TontineRepository tontineRepository;
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private EcheancePretRepository echeancePretRepository;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();

    @InjectMocks
    private PretService pretService;

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

    // Tontine 6, ACTIVE, mensuelle, avec ce solde de caisse de prêts.
    private Tontine tontine(String solde) {
        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setId(1L);
        gestionnaire.setTelephone("770000101");
        Tontine t = new Tontine();
        t.setId(6L);
        t.setGestionnaire(gestionnaire);
        t.setStatut(StatutTontine.ACTIVE);
        t.setFrequence(FrequenceTontine.MOIS);
        t.setIntervalle(1);
        t.setSoldeCaissePret(new BigDecimal(solde));
        return t;
    }

    private Participation participation(StatutParticipation statut) {
        Utilisateur membre = new Utilisateur();
        membre.setId(3L);
        Participation p = new Participation();
        p.setMembre(membre);
        p.setStatut(statut);
        return p;
    }

    private DemandePretDTO demande(String montant, int nbEcheances) {
        DemandePretDTO d = new DemandePretDTO();
        d.setMembreId(3L);
        d.setMontant(new BigDecimal(montant));
        d.setNbEcheances(nbEcheances);
        d.setDateDebutRemboursement(LocalDate.of(2026, 1, 31));
        return d;
    }

    // Tous les contrôles passent : tontine trouvée, membre ACTIF, pas de
    // prêt en cours ; save() renvoie l'objet reçu.
    private void accordPossible(Tontine tontine) {
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        when(participationRepository.findByTontineIdAndMembreId(6L, 3L))
                .thenReturn(Optional.of(participation(StatutParticipation.ACTIF)));
        when(pretRepository.save(any())).thenAnswer(appel -> appel.getArgument(0));
    }

    // Les échéances enregistrées, dans l'ordre de save().
    private List<EcheancePret> echeancesEnregistrees(int nb) {
        ArgumentCaptor<EcheancePret> captor = ArgumentCaptor.forClass(EcheancePret.class);
        verify(echeancePretRepository, org.mockito.Mockito.times(nb)).save(captor.capture());
        return captor.getAllValues();
    }

    private Pret pret(Tontine tontine, StatutPret statut) {
        Utilisateur membre = new Utilisateur();
        membre.setId(3L);
        Pret p = new Pret();
        p.setId(20L);
        p.setTontine(tontine);
        p.setMembre(membre);
        p.setGestionnaire(tontine.getGestionnaire());
        p.setStatut(statut);
        return p;
    }

    private EcheancePret echeance(int numero, String du, String paye, StatutEcheancePret statut) {
        EcheancePret e = new EcheancePret();
        e.setNumeroEcheance(numero);
        e.setMontantDu(new BigDecimal(du));
        e.setMontantPaye(new BigDecimal(paye));
        e.setStatut(statut);
        return e;
    }

    private VersementDTO versement(String montant) {
        VersementDTO v = new VersementDTO();
        v.setMontant(new BigDecimal(montant));
        return v;
    }

    // ------------------------------------------------------ accorder : refus

    @Test
    void accorder_tontineInexistante_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.empty());

        assertThrows(TontineIntrouvableException.class,
                () -> pretService.accorderPret(6L, demande("10000", 2)));
    }

    // Frontière SaaS : un autre gestionnaire ne prête pas sur ma caisse.
    @Test
    void accorder_parUnAutreGestionnaire_donne403SansRienEnregistrer() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine("100000")));

        assertThrows(AccesRefuseException.class,
                () -> pretService.accorderPret(6L, demande("10000", 2)));
        verify(pretRepository, never()).save(any());
    }

    @Test
    void accorder_surUneTontineNonActive_donne409() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine("100000");
        tontine.setStatut(StatutTontine.SUSPENDUE);
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));

        assertThrows(TontineNonActiveException.class,
                () -> pretService.accorderPret(6L, demande("10000", 2)));
    }

    @Test
    void accorder_aUnInconnuDeLaTontine_donne400() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine("100000")));
        when(participationRepository.findByTontineIdAndMembreId(6L, 3L)).thenReturn(Optional.empty());

        assertThrows(MembreNonParticipantException.class,
                () -> pretService.accorderPret(6L, demande("10000", 2)));
    }

    // filter() : la participation existe, mais le membre est SORTI.
    @Test
    void accorder_aUnMembreSorti_donne400() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine("100000")));
        when(participationRepository.findByTontineIdAndMembreId(6L, 3L))
                .thenReturn(Optional.of(participation(StatutParticipation.SORTI)));

        assertThrows(MembreNonParticipantException.class,
                () -> pretService.accorderPret(6L, demande("10000", 2)));
    }

    // Un prêt ACTIF ou EN_RETARD dans CETTE tontine bloque un 2e prêt.
    @Test
    void accorder_alorsQuUnPretEstEnCours_donne409() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine("100000")));
        when(participationRepository.findByTontineIdAndMembreId(6L, 3L))
                .thenReturn(Optional.of(participation(StatutParticipation.ACTIF)));
        when(pretRepository.existsByMembreIdAndTontineIdAndStatutIn(3L, 6L,
                List.of(StatutPret.ACTIF, StatutPret.EN_RETARD))).thenReturn(true);

        assertThrows(PretDejaEnCoursException.class,
                () -> pretService.accorderPret(6L, demande("10000", 2)));
        verify(pretRepository, never()).save(any());
    }

    @Test
    void accorder_plusQueLeSolde_donne409SansDebiterLaCaisse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine("50000");
        when(tontineRepository.findById(6L)).thenReturn(Optional.of(tontine));
        when(participationRepository.findByTontineIdAndMembreId(6L, 3L))
                .thenReturn(Optional.of(participation(StatutParticipation.ACTIF)));

        assertThrows(CaisseInsuffisanteException.class,
                () -> pretService.accorderPret(6L, demande("50001", 2)));
        assertEquals(0, new BigDecimal("50000").compareTo(tontine.getSoldeCaissePret()));
        verify(tontineRepository, never()).save(any());
    }

    // ---------------------------------------------------- accorder : succès

    // Tout le solde peut être prêté : la caisse tombe à 0, jamais négative.
    @Test
    void accorder_toutLeSolde_debiteLaCaisseAZero() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine("50000");
        accordPossible(tontine);

        PretDTO resultat = pretService.accorderPret(6L, demande("50000", 1));

        assertEquals(0, BigDecimal.ZERO.compareTo(tontine.getSoldeCaissePret()));
        verify(tontineRepository).save(tontine);
        assertEquals(StatutPret.ACTIF, resultat.getStatut());
        assertEquals(3L, resultat.getMembreId());
        assertEquals(1L, resultat.getGestionnaireId());
    }

    // Saisie en % : 100 000 à 10 % → intérêt 10 000 F.
    @Test
    void accorder_avecUnTaux_calculeLInteretEnFrancs() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        accordPossible(tontine("100000"));
        DemandePretDTO demande = demande("100000", 5);
        demande.setTauxInteret(new BigDecimal("10"));

        PretDTO resultat = pretService.accorderPret(6L, demande);

        assertEquals(0, new BigDecimal("10000").compareTo(resultat.getMontantInteret()));
        assertEquals(0, new BigDecimal("10").compareTo(resultat.getTauxInteret()));
    }

    // Taux arrondi au franc (FCFA sans centimes) : 1 001 × 10,5 % = 105,105 → 105.
    @Test
    void accorder_avecUnTaux_arrondiAuFranc() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        accordPossible(tontine("100000"));
        DemandePretDTO demande = demande("1001", 1);
        demande.setTauxInteret(new BigDecimal("10.5"));

        PretDTO resultat = pretService.accorderPret(6L, demande);

        assertEquals(0, new BigDecimal("105").compareTo(resultat.getMontantInteret()));
    }

    // Saisie en francs : pris tel quel, pas de taux enregistré.
    @Test
    void accorder_avecUnMontantDInteret_lePrendTelQuel() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        accordPossible(tontine("100000"));
        DemandePretDTO demande = demande("100000", 5);
        demande.setMontantInteret(new BigDecimal("7000"));

        PretDTO resultat = pretService.accorderPret(6L, demande);

        assertEquals(0, new BigDecimal("7000").compareTo(resultat.getMontantInteret()));
        assertNull(resultat.getTauxInteret());
    }

    @Test
    void accorder_sansInteret_donneZero() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        accordPossible(tontine("100000"));

        PretDTO resultat = pretService.accorderPret(6L, demande("100000", 5));

        assertEquals(0, BigDecimal.ZERO.compareTo(resultat.getMontantInteret()));
    }

    // 110 000 en 3 : 36 666 + 36 666 + 36 668 (la dernière absorbe le reste).
    @Test
    void accorder_genereLesEcheancesDontLaDerniereAbsorbeLeReste() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        accordPossible(tontine("100000"));
        DemandePretDTO demande = demande("100000", 3);
        demande.setMontantInteret(new BigDecimal("10000"));

        pretService.accorderPret(6L, demande);

        List<EcheancePret> echeances = echeancesEnregistrees(3);
        assertEquals(0, new BigDecimal("36666").compareTo(echeances.get(0).getMontantDu()));
        assertEquals(0, new BigDecimal("36666").compareTo(echeances.get(1).getMontantDu()));
        assertEquals(0, new BigDecimal("36668").compareTo(echeances.get(2).getMontantDu()));
        BigDecimal total = echeances.stream().map(EcheancePret::getMontantDu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("110000").compareTo(total));
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, echeances.get(i).getNumeroEcheance());
            assertEquals(StatutEcheancePret.EN_ATTENTE, echeances.get(i).getStatut());
            assertEquals(0, BigDecimal.ZERO.compareTo(echeances.get(i).getMontantPaye()));
        }
    }

    // Une seule échéance : elle porte tout (cas limite de la boucle <=).
    @Test
    void accorder_uneSeuleEcheance_porteToutLeTotal() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        accordPossible(tontine("100000"));
        DemandePretDTO demande = demande("20000", 1);
        demande.setMontantInteret(new BigDecimal("1000"));

        pretService.accorderPret(6L, demande);

        List<EcheancePret> echeances = echeancesEnregistrees(1);
        assertEquals(0, new BigDecimal("21000").compareTo(echeances.get(0).getMontantDu()));
    }

    // Rythme de la tontine, calculé depuis la date de début : 31 janv.
    // → 28 févr. → 31 mars (pas de dérive à 28 mars).
    @Test
    void accorder_datesAuRythmeDeLaTontineSansDeriveEnFinDeMois() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        accordPossible(tontine("100000"));

        pretService.accorderPret(6L, demande("30000", 3));

        List<EcheancePret> echeances = echeancesEnregistrees(3);
        assertEquals(LocalDate.of(2026, 1, 31), echeances.get(0).getDateEcheance());
        assertEquals(LocalDate.of(2026, 2, 28), echeances.get(1).getDateEcheance());
        assertEquals(LocalDate.of(2026, 3, 31), echeances.get(2).getDateEcheance());
    }

    // Tontine « toutes les 2 semaines » : échéances espacées de 14 jours.
    @Test
    void accorder_tontineToutesLesDeuxSemaines_espaceLesEcheances() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine("100000");
        tontine.setFrequence(FrequenceTontine.SEMAINE);
        tontine.setIntervalle(2);
        accordPossible(tontine);

        pretService.accorderPret(6L, demande("30000", 2));

        List<EcheancePret> echeances = echeancesEnregistrees(2);
        assertEquals(LocalDate.of(2026, 1, 31), echeances.get(0).getDateEcheance());
        assertEquals(LocalDate.of(2026, 2, 14), echeances.get(1).getDateEcheance());
    }

    // ---------------------------------------------------- rembourser : refus

    @Test
    void rembourser_pretInexistant_donne404() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(pretRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(PretIntrouvableException.class,
                () -> pretService.rembourser(20L, versement("1000")));
    }

    @Test
    void rembourser_parUnAutreGestionnaire_donne403() {
        connecter("770000102", "ROLE_GESTIONNAIRE");
        when(pretRepository.findById(20L)).thenReturn(Optional.of(pret(tontine("0"), StatutPret.ACTIF)));

        assertThrows(AccesRefuseException.class,
                () -> pretService.rembourser(20L, versement("1000")));
        verify(echeancePretRepository, never()).save(any());
    }

    @Test
    void rembourser_unPretDejaRembourse_donne409() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(pretRepository.findById(20L)).thenReturn(Optional.of(pret(tontine("0"), StatutPret.REMBOURSE)));

        assertThrows(PretDejaRembourseException.class,
                () -> pretService.rembourser(20L, versement("1000")));
    }

    // Reste dû 10 000 + 5 000 - 2 000 = 13 000 : 13 001 refusé, rien ne bouge.
    @Test
    void rembourser_plusQueLeResteDu_donne400SansRienEnregistrer() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine("0");
        when(pretRepository.findById(20L)).thenReturn(Optional.of(pret(tontine, StatutPret.ACTIF)));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(20L, StatutEcheancePret.PAYE))
                .thenReturn(List.of(echeance(1, "10000", "0", StatutEcheancePret.EN_ATTENTE),
                        echeance(2, "5000", "2000", StatutEcheancePret.EN_ATTENTE)));

        assertThrows(MontantPayeSuperieurAuDuException.class,
                () -> pretService.rembourser(20L, versement("13001")));
        verify(echeancePretRepository, never()).save(any());
        assertEquals(0, BigDecimal.ZERO.compareTo(tontine.getSoldeCaissePret()));
    }

    // -------------------------------------------------- rembourser : succès

    // 50 000 sur 36 666 / 36 666 / 36 668 : la 1re est soldée, la 2e reçoit
    // 13 334, la 3e rien. La caisse reçoit les 50 000. Prêt toujours ACTIF.
    @Test
    void rembourser_repartitSurLesEcheancesLesPlusAnciennesDAbord() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine("1000");
        Pret pret = pret(tontine, StatutPret.ACTIF);
        EcheancePret e1 = echeance(1, "36666", "0", StatutEcheancePret.EN_ATTENTE);
        EcheancePret e2 = echeance(2, "36666", "0", StatutEcheancePret.EN_ATTENTE);
        EcheancePret e3 = echeance(3, "36668", "0", StatutEcheancePret.EN_ATTENTE);
        when(pretRepository.findById(20L)).thenReturn(Optional.of(pret));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(20L, StatutEcheancePret.PAYE))
                .thenReturn(List.of(e1, e2, e3));

        PretDTO resultat = pretService.rembourser(20L, versement("50000"));

        assertEquals(StatutEcheancePret.PAYE, e1.getStatut());
        assertEquals(0, new BigDecimal("36666").compareTo(e1.getMontantPaye()));
        assertEquals(StatutEcheancePret.EN_ATTENTE, e2.getStatut());
        assertEquals(0, new BigDecimal("13334").compareTo(e2.getMontantPaye()));
        assertEquals(0, BigDecimal.ZERO.compareTo(e3.getMontantPaye()));
        verify(echeancePretRepository, never()).save(e3);
        assertEquals(0, new BigDecimal("51000").compareTo(tontine.getSoldeCaissePret()));
        assertEquals(StatutPret.ACTIF, resultat.getStatut());
        verify(pretRepository, never()).save(any());
    }

    // Tout le reste d'un coup (intérêt compris) → REMBOURSE, caisse créditée.
    @Test
    void rembourser_toutLeReste_passeRembourse() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Tontine tontine = tontine("0");
        Pret pret = pret(tontine, StatutPret.ACTIF);
        EcheancePret e2 = echeance(2, "36666", "13334", StatutEcheancePret.EN_ATTENTE);
        EcheancePret e3 = echeance(3, "36668", "0", StatutEcheancePret.EN_ATTENTE);
        when(pretRepository.findById(20L)).thenReturn(Optional.of(pret));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(20L, StatutEcheancePret.PAYE))
                .thenReturn(List.of(e2, e3));

        PretDTO resultat = pretService.rembourser(20L, versement("60000"));

        assertEquals(StatutEcheancePret.PAYE, e2.getStatut());
        assertEquals(StatutEcheancePret.PAYE, e3.getStatut());
        assertEquals(StatutPret.REMBOURSE, resultat.getStatut());
        verify(pretRepository).save(pret);
        assertEquals(0, new BigDecimal("60000").compareTo(tontine.getSoldeCaissePret()));
    }

    // Prêt EN_RETARD : il rattrape sa seule échéance en retard → ACTIF.
    @Test
    void rembourser_rattrapeTousSesRetards_redevientActif() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Pret pret = pret(tontine("0"), StatutPret.EN_RETARD);
        when(pretRepository.findById(20L)).thenReturn(Optional.of(pret));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(20L, StatutEcheancePret.PAYE))
                .thenReturn(new ArrayList<>(List.of(echeance(1, "10000", "0", StatutEcheancePret.EN_RETARD),
                        echeance(2, "10000", "0", StatutEcheancePret.EN_ATTENTE))));
        when(echeancePretRepository.existsByPretIdAndStatut(20L, StatutEcheancePret.EN_RETARD)).thenReturn(false);

        PretDTO resultat = pretService.rembourser(20L, versement("10000"));

        assertEquals(StatutPret.ACTIF, resultat.getStatut());
        verify(pretRepository).save(pret);
    }

    // Rattrapage partiel : il reste une échéance EN_RETARD → reste EN_RETARD.
    @Test
    void rembourser_rattrapagePartiel_resteEnRetard() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        Pret pret = pret(tontine("0"), StatutPret.EN_RETARD);
        EcheancePret e1 = echeance(1, "10000", "0", StatutEcheancePret.EN_RETARD);
        when(pretRepository.findById(20L)).thenReturn(Optional.of(pret));
        when(echeancePretRepository.findByPretIdAndStatutNotOrderByNumeroEcheanceAsc(20L, StatutEcheancePret.PAYE))
                .thenReturn(List.of(e1, echeance(2, "10000", "0", StatutEcheancePret.EN_ATTENTE)));
        when(echeancePretRepository.existsByPretIdAndStatut(20L, StatutEcheancePret.EN_RETARD)).thenReturn(true);

        PretDTO resultat = pretService.rembourser(20L, versement("4000"));

        assertEquals(StatutEcheancePret.EN_RETARD, e1.getStatut());
        assertEquals(StatutPret.EN_RETARD, resultat.getStatut());
        verify(pretRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- lecture

    @Test
    void lister_parUnGestionnaire_neLitQueLesPretsDeSesTontines() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(pretRepository.findByTontineGestionnaireTelephone("770000101")).thenReturn(List.of());

        pretService.listPret();

        verify(pretRepository, never()).findAll();
        verify(pretRepository, never()).findByMembreTelephone(anyString());
    }

    // Dette = info privée : le membre ne voit que SES prêts.
    @Test
    void lister_parUnMembre_neLitQueSesPropresPrets() {
        connecter("771234566", "ROLE_MEMBRE");
        when(pretRepository.findByMembreTelephone("771234566")).thenReturn(List.of());

        pretService.listPret();

        verify(pretRepository, never()).findAll();
        verify(pretRepository, never()).findByTontineGestionnaireTelephone(anyString());
        verify(pretRepository).findByMembreTelephone(eq("771234566"));
    }
}
