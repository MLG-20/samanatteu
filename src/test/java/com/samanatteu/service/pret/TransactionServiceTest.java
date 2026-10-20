package com.samanatteu.service.pret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.samanatteu.dto.pret.TransactionDTO;
import com.samanatteu.entity.pret.Transaction;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.exception.AccesRefuseException;
import com.samanatteu.exception.pret.TransactionIntrouvableException;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.ModePaiement;
import com.samanatteu.enums.pret.SensTransaction;
import com.samanatteu.enums.pret.TypeTransaction;
import com.samanatteu.repository.pret.TransactionRepository;
import com.samanatteu.security.UtilisateurConnecte;

// Journal financier : journaliser (sens déduit du type, champs recopiés,
// dates serveur) et lecture filtrée par rôle. Tontine 6 gérée par 770000101,
// membre id 3 (771234566).
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Spy
    private UtilisateurConnecte utilisateurConnecte = new UtilisateurConnecte();
    // Un vrai objet (pas un faux) : la fabrication du PDF n'a aucune dépendance,
    // et le test veut un vrai PDF (c'est lui qu'on relit ensuite).
    @Spy
    private RecuPDFService recuPDFService = new RecuPDFService();

    @InjectMocks
    private TransactionService transactionService;

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

    private Utilisateur membre() {
        Utilisateur m = new Utilisateur();
        m.setId(3L);
        return m;
    }

    private Tontine tontine() {
        Tontine t = new Tontine();
        t.setId(6L);
        return t;
    }

    // La ligne réellement passée à save() (ArgumentCaptor = "attrape" l'argument).
    private Transaction ligneEnregistree() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    // ------------------------------------------------------------- journaliser

    // Le sens n'est jamais donné par l'appelant : il est déduit du type,
    // vu de la caisse de la tontine. Une ligne par valeur de l'enum.
    @ParameterizedTest
    @CsvSource({
            "COTISATION, ENTRANT",
            "REMBOURSEMENT, ENTRANT",
            "PENALITE, ENTRANT",
            "GAIN, SORTANT",
            "PRET, SORTANT" })
    void journaliser_deduitLeSensDuType(TypeTransaction type, SensTransaction sensAttendu) {
        transactionService.journaliser(membre(), tontine(), type, new BigDecimal("1000"),
                null, null, 1L, null);

        assertEquals(sensAttendu, ligneEnregistree().getSens());
    }

    // Tous les champs reçus sont recopiés tels quels ; les deux dates sont
    // fixées par le serveur.
    @Test
    void journaliser_recopieLesChampsEtDateLeMouvement() {
        Utilisateur membre = membre();
        Tontine tontine = tontine();

        transactionService.journaliser(membre, tontine, TypeTransaction.COTISATION,
                new BigDecimal("10500"), ModePaiement.WAVE, "W-123", 7L, "note");

        Transaction ligne = ligneEnregistree();
        assertEquals(membre, ligne.getMembre());
        assertEquals(tontine, ligne.getTontine());
        assertEquals(TypeTransaction.COTISATION, ligne.getType());
        assertEquals(0, new BigDecimal("10500").compareTo(ligne.getMontant()));
        assertEquals(ModePaiement.WAVE, ligne.getModePaiement());
        assertEquals("W-123", ligne.getReference());
        assertEquals(7L, ligne.getReferenceId());
        assertEquals("note", ligne.getDescription());
        assertNotNull(ligne.getDateTransaction());
        assertNotNull(ligne.getCreatedAt());
    }

    // ------------------------------------------------------------------ recu PDF

    // Une ligne de journal du membre 771234566, dans la tontine 6 gérée par 770000101.
    private Transaction ligneDuJournal() {
        Utilisateur membre = new Utilisateur();
        membre.setId(3L);
        membre.setTelephone("+221771234566");
        membre.setPrenom("Awa");
        membre.setNom("Diop");

        Utilisateur gestionnaire = new Utilisateur();
        gestionnaire.setTelephone("+221770000101");

        Tontine tontine = new Tontine();
        tontine.setId(6L);
        tontine.setNom("Natt des femmes");
        tontine.setGestionnaire(gestionnaire);

        Transaction transaction = new Transaction();
        transaction.setId(9L);
        transaction.setMembre(membre);
        transaction.setTontine(tontine);
        transaction.setMontant(new BigDecimal("15000"));
        transaction.setDateTransaction(java.time.LocalDateTime.of(2026, 10, 20, 15, 30));
        return transaction;
    }

    // Règle : le membre qui a payé peut télécharger SON reçu.
    @Test
    void genererRecu_leMembreConcerneLobtient() {
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(ligneDuJournal()));
        connecter("+221771234566", "ROLE_MEMBRE");

        byte[] pdf = transactionService.genererRecu(9L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    // Règle : la gestionnaire de la tontine peut aussi (elle donne le reçu papier).
    @Test
    void genererRecu_laGestionnaireDeLaTontineLObtient() {
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(ligneDuJournal()));
        connecter("+221770000101", "ROLE_GESTIONNAIRE");

        byte[] pdf = transactionService.genererRecu(9L);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    // Règle : un autre membre n'a PAS le droit de lire le reçu d'autrui.
    @Test
    void genererRecu_unAutreMembreEstRefuse() {
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(ligneDuJournal()));
        connecter("+221779999999", "ROLE_MEMBRE");

        assertThrows(AccesRefuseException.class, () -> transactionService.genererRecu(9L));
    }

    // Règle : l'ADMIN n'a pas le droit non plus (il ne voit pas le contenu des
    // tontines de ses clients).
    @Test
    void genererRecu_unAdminEstRefuse() {
        when(transactionRepository.findById(9L)).thenReturn(Optional.of(ligneDuJournal()));
        connecter("+221770000099", "ROLE_ADMIN");

        assertThrows(AccesRefuseException.class, () -> transactionService.genererRecu(9L));
    }

    // Règle : une ligne de journal inconnue donne un 404, pas un 500.
    @Test
    void genererRecu_ligneInconnueDonne404() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());
        connecter("+221771234566", "ROLE_MEMBRE");

        assertThrows(TransactionIntrouvableException.class, () -> transactionService.genererRecu(99L));
    }

    // ----------------------------------------------------------------- lecture

    private Transaction ligne() {
        Transaction t = new Transaction();
        t.setMembre(membre());
        t.setTontine(tontine());
        t.setType(TypeTransaction.COTISATION);
        t.setSens(SensTransaction.ENTRANT);
        t.setMontant(new BigDecimal("1000"));
        return t;
    }

    // Gestionnaire : le journal de SES tontines, jamais findAll.
    @Test
    void lister_parUnGestionnaire_neLitQueLeJournalDeSesTontines() {
        connecter("770000101", "ROLE_GESTIONNAIRE");
        when(transactionRepository.findByTontineGestionnaireTelephoneOrderByDateTransactionDesc("770000101"))
                .thenReturn(List.of(ligne()));

        List<TransactionDTO> resultat = transactionService.listTransactions();

        assertEquals(1, resultat.size());
        assertEquals(6L, resultat.get(0).getTontineId());
        verify(transactionRepository, never()).findAll();
        verify(transactionRepository, never()).findByMembreTelephoneOrderByDateTransactionDesc(any());
    }

    // Membre (option 1) : SES lignes seulement, pas celles des autres membres.
    @Test
    void lister_parUnMembre_neLitQueSesPropresLignes() {
        connecter("771234566", "ROLE_MEMBRE");
        when(transactionRepository.findByMembreTelephoneOrderByDateTransactionDesc("771234566"))
                .thenReturn(List.of(ligne()));

        List<TransactionDTO> resultat = transactionService.listTransactions();

        assertEquals(1, resultat.size());
        assertEquals(3L, resultat.get(0).getMembreId());
        verify(transactionRepository, never()).findAll();
        verify(transactionRepository, never()).findByTontineGestionnaireTelephoneOrderByDateTransactionDesc(any());
    }
}
