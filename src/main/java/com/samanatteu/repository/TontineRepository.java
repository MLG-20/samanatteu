package com.samanatteu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Tontine;

public interface TontineRepository extends JpaRepository<Tontine, Long> {
    // Spring Data JPA génère automatiquement save(), findById(), findAll(),
    // deleteById()...
    List<Tontine> findByGestionnaireTelephone(String telephone);
}
