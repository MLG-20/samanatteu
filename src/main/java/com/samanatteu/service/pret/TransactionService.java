package com.samanatteu.service.pret;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.pret.TransactionDTO;
import com.samanatteu.entity.Transaction;
import com.samanatteu.exception.MontantInvalideException;
import com.samanatteu.repository.TransactionRepository;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Lister
    public List<TransactionDTO> listTransactions() {
        return transactionRepository.findAll() // 1. List<Transaction> brute depuis la base (avec motDePasse)
                .stream() // 2. transforme la liste en flux traitable élément par élément
                .map(this::convertiTransactionDTO) // 3. applique la conversion à CHAQUE Transaction -> TransactionDTO
                // (sans motDePasse)
                .toList(); // 4. reconstitue une vraie List<TransactionDTO> à partir du flux
    }

    // créer
    public TransactionDTO createTransaction(Transaction transaction) {
        if (transaction.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontantInvalideException(transaction.getMontant());
        }
        Transaction enregistre = transactionRepository.save(transaction);
        return convertiTransactionDTO(enregistre);
    }

    // update
    public Optional<TransactionDTO> updateTransaction(Long id, Transaction transactionModifier) {
        // findById(id) renvoie un Optional<Transaction> : vide si l'id n'existe pas,
        // rempli sinon.
        // .map(...) ne s'exécute QUE si l'Optional est rempli — sinon il reste vide tel
        // quel (pas de NullPointerException).
        return transactionRepository.findById(id).map(transactionExsitante -> {
            // transactionExsitante = l'entité déjà en base (trouvée par findById).
            // transactionModifier = les nouvelles valeurs envoyées par le client (paramètre
            // de la méthode).
            // On recopie les nouvelles valeurs DANS l'entité existante, champ par champ.
            transactionExsitante.setMembre(transactionModifier.getMembre());
            transactionExsitante.setTontine(transactionModifier.getTontine());
            transactionExsitante.setType(transactionModifier.getType());
            transactionExsitante.setMontant(transactionModifier.getMontant());
            transactionExsitante.setSens(transactionModifier.getSens());
            transactionExsitante.setReferenceId(transactionModifier.getReferenceId());
            transactionExsitante.setDescription(transactionModifier.getDescription());
            transactionExsitante.setDateTransaction(transactionModifier.getDateTransaction());
            transactionExsitante.setCreatedAt(transactionModifier.getCreatedAt());

            // save() persiste les changements en base ET renvoie l'entité Transaction à
            // jour
            // (avec motDePasse).
            Transaction enregistree = transactionRepository.save(transactionExsitante);
            // On ne renvoie JAMAIS l'entité brute au client : conversion en DTO juste avant
            // de sortir (sans motDePasse).
            // Comme on est dans un .map(), ce retour devient automatiquement le contenu de
            // l'Optional<TransactionDTO>.
            return convertiTransactionDTO(enregistree);
        });
    }

    // Delete
    public boolean deleteTransaction(Long id) {
        if (transactionRepository.existsById(id)) {
            transactionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private TransactionDTO convertiTransactionDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setMembreId(transaction.getMembre().getId());
        dto.setTontineId(transaction.getTontine().getId());
        dto.setType(transaction.getType());
        dto.setMontant(transaction.getMontant());
        dto.setSens(transaction.getSens());
        dto.setReferenceId(transaction.getReferenceId());
        dto.setDescription(transaction.getDescription());
        dto.setDateTransaction(transaction.getDateTransaction());
        dto.setCreatedAt(transaction.getCreatedAt());

        return dto;
    }
}
