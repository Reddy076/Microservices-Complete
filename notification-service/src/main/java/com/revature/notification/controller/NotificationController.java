package com.revature.notification.controller;

import com.revature.notification.dto.NotificationDTO;
import com.revature.notification.dto.response.MessageResponse;
import com.revature.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.revature.notification.dto.response.UnreadCountResponse;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<List<NotificationDTO>> getNotifications(@RequestHeader("X-User-Name") String username) {
    return ResponseEntity.ok(notificationService.getNotifications(username));
  }

  @GetMapping("/unread-count")
  public ResponseEntity<UnreadCountResponse> getUnreadCount(@RequestHeader("X-User-Name") String username) {
    long count = notificationService.getUnreadCount(username);
    return ResponseEntity.ok(new UnreadCountResponse(count));
  }

  @PutMapping("/{id}/read")
  public ResponseEntity<MessageResponse> markAsRead(@RequestHeader("X-User-Name") String username, @PathVariable Long id) {
    notificationService.markAsRead(username, id);
    return ResponseEntity.ok(new MessageResponse("Notification marked as read"));
  }

  @PutMapping("/mark-all-read")
  public ResponseEntity<MessageResponse> markAllAsRead(@RequestHeader("X-User-Name") String username) {
    notificationService.markAllAsRead(username);
    return ResponseEntity.ok(new MessageResponse("All notifications marked as read"));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<MessageResponse> deleteNotification(@RequestHeader("X-User-Name") String username, @PathVariable Long id) {
    notificationService.deleteNotification(username, id);
    return ResponseEntity.ok(new MessageResponse("Notification deleted"));
  }
}





