package com.samanatteu.repository.pret;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.pret.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Lecture filtrée du journal, plus récent d'abord (comme un relevé).
    // Gestionnaire : lignes de SES tontines. Membre : SES lignes seulement.
    List<Transaction> findByTontineGestionnaireTelephoneOrderByDateTransactionDesc(String telephone);

    List<Transaction> findByMembreTelephoneOrderByDateTransactionDesc(String telephone);

}
