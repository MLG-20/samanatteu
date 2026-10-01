package com.samanatteu.service.pret;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.pret.TransactionDTO;
import com.samanatteu.entity.pret.Transaction;
import com.samanatteu.entity.tontine.Tontine;
import com.samanatteu.entity.utilisateur.Utilisateur;
import com.samanatteu.enums.ModePaiement;
import com.samanatteu.enums.pret.SensTransaction;
import com.samanatteu.enums.pret.TypeTransaction;
import com.samanatteu.enums.utilisateur.RoleUtilisateur;
import com.samanatteu.repository.pret.TransactionRepository;
import com.samanatteu.security.UtilisateurConnecte;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final UtilisateurConnecte utilisateurConnecte;

    public TransactionService(TransactionRepository transactionRepository, UtilisateurConnecte utilisateurConnecte) {
        this.transactionRepository = transactionRepository;
        this.utilisateurConnecte = utilisateurConnecte;
    }

    // Lecture filtrée (option 1) : gestionnaire → journal de SES tontines ;
    // membre → SES lignes seulement (prêts privés). ADMIN refusé avant (403).
    public List<TransactionDTO> listTransactions() {
        boolean estGestionnaire = utilisateurConnecte.aLeRole(RoleUtilisateur.GESTIONNAIRE);

        List<Transaction> transactions = estGestionnaire
        ? transactionRepository.findByTontineGestionnaireTelephoneOrderByDateTransactionDesc(utilisateurConnecte.telephone())
        :transactionRepository.findByMembreTelephoneOrderByDateTransactionDesc(utilisateurConnecte.telephone());
        return transactions.stream()
                .map(this::convertiTransactionDTO)
                .toList();
    }

    // Seul point d'écriture du journal : sens et dates sont déduits ici, pour
    // qu'aucun appelant ne puisse les oublier ou se tromper.
    public void journaliser(Utilisateur membre, Tontine tontine, TypeTransaction type,
            BigDecimal montant, ModePaiement modePaiement, String reference,
            Long referenceId, String description) {
        // Sens vu de la tontine. Pas de default : un nouveau type ajouté à
        // l'enum ne compilera pas tant qu'on ne lui aura pas donné un sens.
        SensTransaction sens = switch (type) {
            case COTISATION, REMBOURSEMENT, PENALITE -> SensTransaction.ENTRANT;
            case GAIN, PRET -> SensTransaction.SORTANT;

        };

        Transaction transaction = new Transaction();
        transaction.setMembre(membre);
        transaction.setTontine(tontine);
        transaction.setType(type);
        transaction.setSens(sens);
        transaction.setMontant(montant);
        transaction.setModePaiement(modePaiement);
        transaction.setReference(reference);
        transaction.setReferenceId(referenceId);
        transaction.setDescription(description);
        transaction.setDateTransaction(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

    }

    private TransactionDTO convertiTransactionDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setMembreId(transaction.getMembre().getId());
        dto.setTontineId(transaction.getTontine().getId());
        dto.setType(transaction.getType());
        dto.setMontant(transaction.getMontant());
        dto.setSens(transaction.getSens());
        dto.setModePaiement(transaction.getModePaiement());
        dto.setReference(transaction.getReference());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setDescription(transaction.getDescription());
        dto.setDateTransaction(transaction.getDateTransaction());
        dto.setCreatedAt(transaction.getCreatedAt());

        return dto;
    }
}
