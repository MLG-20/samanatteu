package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.ImportMembre;

public interface ImportMembreRepository extends JpaRepository <ImportMembre, Long>{
    // Spring Data JPA génère automatiquement save(), findById(), findAll(), deleteById()...
}
