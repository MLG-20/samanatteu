package com.samanatteu.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long>{
}
