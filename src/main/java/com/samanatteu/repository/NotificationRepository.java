package com.samanatteu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.samanatteu.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long>{
}
