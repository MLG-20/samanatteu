package com.samanatteu.service.pret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

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
