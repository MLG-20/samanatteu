package com.samanatteu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.notification.NotificationDTO;
import com.samanatteu.entity.Notification;
import com.samanatteu.service.notification.NotificationService;

@RequestMapping("/notification")
@RestController
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // --- Lister ---
    @GetMapping
    public List<NotificationDTO> listNotification() {
        return notificationService.listNotification();
    }

    // --- Créer ---
    @PostMapping

    public NotificationDTO createNotification(@RequestBody Notification notification) {
        return notificationService.createNotification(notification);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<NotificationDTO> updateNotification(@PathVariable Long id,
            @RequestBody Notification notificationModifier) {
        return notificationService.updateNotification(id, notificationModifier)
               .map(ResponseEntity::ok)
               .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        if (notificationService.deleteNotification(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
