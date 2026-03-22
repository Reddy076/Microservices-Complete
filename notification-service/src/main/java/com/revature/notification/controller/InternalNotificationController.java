package com.revature.notification.controller;

import com.revature.notification.model.Notification.NotificationType;
import com.revature.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints called by vault-service via NotificationClient Feign.
 * Creates notifications triggered by vault operations (entry created/updated/deleted etc.)
 */
@RestController
@RequestMapping("/api/notifications/internal")
@RequiredArgsConstructor
public class InternalNotificationController {

    private static final Logger log = LoggerFactory.getLogger(InternalNotificationController.class);

    private final NotificationService notificationService;

    /**
     * Called by vault-service when vault operations occur.
     * POST /api/notifications/internal/create?username=Reddy&type=ACCOUNT_ACTIVITY&title=...&message=...
     */
    @PostMapping("/create")
    public ResponseEntity<Void> createNotification(
            @RequestParam String username,
            @RequestParam String type,
            @RequestParam String title,
            @RequestParam String message) {
        try {
            NotificationType notificationType = NotificationType.valueOf(type);
            notificationService.createNotification(username, notificationType, title, message);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification type '{}' for user {}", type, username);
        } catch (Exception e) {
            log.error("Failed to create notification for user {}: {}", username, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
