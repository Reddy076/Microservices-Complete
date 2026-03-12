package com.revature.user.service.auth;

import com.revature.user.dto.response.SessionResponse;
import com.revature.user.exception.ResourceNotFoundException;
import com.revature.user.model.user.User;
import com.revature.user.model.user.UserSession;
import com.revature.user.repository.UserSessionRepository;
import com.revature.user.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.revature.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

  private final UserSessionRepository userSessionRepository;
  private final UserRepository userRepository;
  private final ClientIpUtil clientIpUtil;

  @Transactional
  public void createSession(User user, String token, HttpServletRequest request, String location,
      String deviceFingerprint) {
    log.info("Creating new session for user: {}", user.getUsername());

    String ipAddress = clientIpUtil.getClientIpAddress(request);
    String userAgent = request.getHeader("User-Agent");

    UserSession session = UserSession.builder()
        .user(user)
        .token(token)
        .ipAddress(ipAddress)
        .deviceInfo(userAgent != null ? userAgent : "Unknown Device")
        .deviceFingerprint(deviceFingerprint)
        .location(location)
        .isActive(true)
        .lastAccessedAt(LocalDateTime.now())

        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();

    userSessionRepository.save(session);
    log.info("Session created successfully for user: {}", user.getUsername());
  }

  @Transactional(readOnly = true)
  public boolean isSessionActive(String token) {
    return userSessionRepository.findFirstByTokenOrderByIdDesc(token)
        .map(UserSession::isActive)
        .orElse(false);
  }

  @Transactional
  public void terminateSession(Long sessionId, String username) {
    log.info("Terminating session {} for user {}", sessionId, username);
    UserSession session = userSessionRepository.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

    if (!session.getUser().getUsername().equals(username)) {
      log.warn("User {} attempted to terminate session {} belonging to another user", username, sessionId);
      throw new ResourceNotFoundException("Session", "id", sessionId);
    }

    session.setActive(false);
    userSessionRepository.save(session);
    log.info("Session {} terminated successfully", sessionId);
  }

  @Transactional
  public void terminateSessionByToken(String token) {
    userSessionRepository.findFirstByTokenOrderByIdDesc(token).ifPresent(session -> {
      session.setActive(false);
      userSessionRepository.save(session);
      log.info("Session terminated by token for user {}", session.getUser().getUsername());
    });
  }

  @Transactional
  public void terminateAllUserSessions(String username) {
    log.info("Terminating all sessions for user {}", username);
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrue(user.getId());
    for (UserSession session : activeSessions) {
      session.setActive(false);
    }
    userSessionRepository.saveAll(activeSessions);
    log.info("Terminated {} sessions for user {}", activeSessions.size(), username);
  }

  @Transactional(readOnly = true)
  public List<SessionResponse> getUserSessions(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    return userSessionRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public SessionResponse getCurrentSession(String token) {
    return userSessionRepository.findFirstByTokenOrderByIdDesc(token)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Session", "token", "current"));
  }

  @Transactional
  public SessionResponse extendSession(String token) {
    UserSession session = userSessionRepository.findFirstByTokenOrderByIdDesc(token)
        .orElseThrow(() -> new ResourceNotFoundException("Session", "token", "current"));

    if (!session.isActive()) {
      throw new com.revature.user.exception.AuthenticationException("Cannot extend an inactive session");
    }

    session.setLastAccessedAt(LocalDateTime.now());
    session.setExpiresAt(LocalDateTime.now().plusDays(7));
    userSessionRepository.save(session);
    log.info("Session extended successfully for user: {}", session.getUser().getUsername());

    return mapToResponse(session);
  }

  private SessionResponse mapToResponse(UserSession session) {
    return SessionResponse.builder()
        .id(session.getId())
        .ipAddress(session.getIpAddress())
        .deviceInfo(session.getDeviceInfo())
        .location(session.getLocation())
        .isActive(session.isActive())
        .createdAt(session.getCreatedAt())
        .lastAccessedAt(session.getLastAccessedAt())
        .expiresAt(session.getExpiresAt())
        .build();
  }
}
