package com.samanatteu.repository.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Les notifications du connecté seulement, des plus récentes aux plus anciennes.
    List<Notification> findByDestinataireTelephoneOrderByCreatedAtDesc(String telephone);

}
