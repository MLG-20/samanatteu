package com.samanatteu.repository.site;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.site.MessageContact;

// Les messages du formulaire de contact, lus par l'ADMIN.
public interface MessageContactRepository extends JpaRepository<MessageContact, Long> {

    // Les plus récents d'abord (index message_contact_created_at_idx).
    List<MessageContact> findAllByOrderByCreatedAtDesc();
}
