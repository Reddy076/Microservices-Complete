package com.revature.notification.service;

import com.revature.notification.dto.NotificationDTO;
import com.revature.notification.exception.ResourceNotFoundException;
import com.revature.notification.model.Notification;
import com.revature.notification.model.Notification.NotificationType;

import com.revature.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.revature.notification.client.UserClient;
import com.revature.notification.client.UserClient.UserVaultDetails;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository notificationRepository;
  private final UserClient userClient;
  

  @Transactional
  public void createNotification(String username, NotificationType type,
      String title, String message) {
    UserVaultDetails user = null;
    try {
      user = userClient.getUserDetailsByUsername(username);
    } catch (Exception e) {
      logger.warn("Cannot create notification for unknown user: {}", username);
      return;
    }

    Notification notification = Notification.builder()
        .userId(user.getId())
        .notificationType(type)
        .title(title)
        .message(message)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();
    notificationRepository.save(notification);
  }

  @Transactional(readOnly = true)
  public List<NotificationDTO> getNotifications(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
        .stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public long getUnreadCount(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    long count = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    logger.debug("Unread notification count for user {}: {}", username, count);
    return count;
  }

  @Transactional
  public void markAsRead(String username, Long notificationId) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    if (!notification.getUserId().equals(user.getId())) {
      throw new IllegalArgumentException("Notification does not belong to user");
    }
    notification.setRead(true);
    notificationRepository.save(notification);
    logger.info("Marked notification {} as read for user {}", notificationId, username);
  }

  @Transactional
  public void markAllAsRead(String username) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    List<Notification> unread = notificationRepository
        .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
    logger.info("Marking {} notifications as read for user {}", unread.size(), username);
    unread.forEach(n -> n.setRead(true));
    notificationRepository.saveAll(unread);
    logger.info("Successfully marked all notifications as read for user {}", username);
  }

  @Transactional
  public void deleteNotification(String username, Long notificationId) {
    UserVaultDetails user = userClient.getUserDetailsByUsername(username);
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    if (!notification.getUserId().equals(user.getId())) {
      throw new IllegalArgumentException("Notification does not belong to user");
    }
    notificationRepository.delete(notification);
  }

  private NotificationDTO mapToDTO(Notification notification) {
    return NotificationDTO.builder()
        .id(notification.getId())
        .notificationType(notification.getNotificationType().name())
        .title(notification.getTitle())
        .message(notification.getMessage())
        .isRead(notification.isRead())
        .createdAt(notification.getCreatedAt())
        .build();
  }
}




