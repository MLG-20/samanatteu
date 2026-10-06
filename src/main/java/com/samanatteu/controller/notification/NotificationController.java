package com.samanatteu.controller.notification;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.samanatteu.dto.notification.NotificationDTO;
import com.samanatteu.service.notification.NotificationService;

@RequestMapping("/notification")
@RestController
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Lecture seule : les notifications sont écrites par le serveur (notifier),
    // et une preuve d'envoi ne se supprime pas (CRUD retiré).
    @GetMapping
    public List<NotificationDTO> listNotification() {
        return notificationService.listNotification();
    }

}
