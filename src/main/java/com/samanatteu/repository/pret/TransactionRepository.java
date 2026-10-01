package com.samanatteu.repository.pret;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.pret.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
}
