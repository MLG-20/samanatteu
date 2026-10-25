package com.samanatteu.repository.site;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.site.ParametreSite;

// La ligne unique des paramètres du site : findById(ParametreSite.ID_UNIQUE).
public interface ParametreSiteRepository extends JpaRepository<ParametreSite, Long> {
}
