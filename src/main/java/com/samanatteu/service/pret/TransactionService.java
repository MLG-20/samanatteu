package com.samanatteu.service.pret;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.samanatteu.dto.pret.TransactionDTO;
import com.samanatteu.entity.pret.Transaction;
import com.samanatteu.exception.cotisation.MontantInvalideException;
import com.samanatteu.repository.pret.TransactionRepository;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionDTO> listTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(this::convertiTransactionDTO)
                .toList();
    }

    public TransactionDTO createTransaction(Transaction transaction) {
        if (transaction.getMontant().compareTo(BigDecimal.ZERO) <= 0) {
            throw new MontantInvalideException(transaction.getMontant());
        }
        Transaction enregistre = transactionRepository.save(transaction);
        return convertiTransactionDTO(enregistre);
    }

    public Optional<TransactionDTO> updateTransaction(Long id, Transaction transactionModifier) {
        return transactionRepository.findById(id).map(transactionExsitante -> {
            transactionExsitante.setMembre(transactionModifier.getMembre());
            transactionExsitante.setTontine(transactionModifier.getTontine());
            transactionExsitante.setType(transactionModifier.getType());
            transactionExsitante.setMontant(transactionModifier.getMontant());
            transactionExsitante.setSens(transactionModifier.getSens());
            transactionExsitante.setReferenceId(transactionModifier.getReferenceId());
            transactionExsitante.setDescription(transactionModifier.getDescription());
            transactionExsitante.setDateTransaction(transactionModifier.getDateTransaction());
            transactionExsitante.setCreatedAt(transactionModifier.getCreatedAt());

            Transaction enregistree = transactionRepository.save(transactionExsitante);
            return convertiTransactionDTO(enregistree);
        });
    }

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
