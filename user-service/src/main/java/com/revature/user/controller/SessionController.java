package com.revature.user.controller;

import com.revature.user.dto.response.SessionResponse;
import com.revature.user.dto.response.MessageResponse;
import com.revature.user.service.auth.SessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final SessionService sessionService;

  @GetMapping
  public ResponseEntity<List<SessionResponse>> getActiveSessions(@AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(sessionService.getUserSessions(userDetails.getUsername()));
  }

  @GetMapping("/current")
  public ResponseEntity<SessionResponse> getCurrentSession(@RequestHeader("Authorization") String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      return ResponseEntity.ok(sessionService.getCurrentSession(token));
    }
    return ResponseEntity.notFound().build();
  }

  @PostMapping("/extend")
  public ResponseEntity<SessionResponse> extendSession(@RequestHeader("Authorization") String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      return ResponseEntity.ok(sessionService.extendSession(token));
    }
    return ResponseEntity.badRequest().build();
  }

  @DeleteMapping("/{sessionId}")
  public ResponseEntity<MessageResponse> terminateSession(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal UserDetails userDetails) {
    sessionService.terminateSession(sessionId, userDetails.getUsername());
    return ResponseEntity.ok(new MessageResponse("Session terminated successfully"));
  }

  @DeleteMapping
  public ResponseEntity<MessageResponse> terminateAllSessions(@AuthenticationPrincipal UserDetails userDetails) {
    sessionService.terminateAllUserSessions(userDetails.getUsername());

    return ResponseEntity.ok(new MessageResponse("All sessions terminated successfully"));
  }
}
