package com.samanatteu.entity.commun;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

// @MappedSuperclass = pas de table pour elle : ses 2 colonnes sont
// recopiées dans la table de chaque entité fille. Une seule écriture
// du code dates pour 11 entités, au lieu de 11 copiés-collés.

@MappedSuperclass
@Getter
@Setter
public abstract class Auditable {
    // updatable = false : la date de création ne change jamais après l'INSERT,
    // même si quelqu'un appelle setCreatedAt par accident.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Avant le 1er INSERT : remplit ce qui manque.
    // Les gardes null permettent d'importer de vieilles données avec leurs dates.
    @PrePersist
    void prePersist() {
        LocalDateTime maintenant = LocalDateTime.now();
        if (createdAt == null)
            createdAt = maintenant;
        if (updatedAt == null)
            updatedAt = maintenant;
    }

    // Avant chaque UPDATE : rafraîchit updatedAt. Jamais appelé à la création.
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
