package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
}
