package com.revature.security.controller;

import com.revature.security.dto.response.PasswordAgeResponse;
import com.revature.security.dto.response.PasswordHealthMetricsResponse;
import com.revature.security.dto.response.ReusedPasswordResponse;
import com.revature.security.dto.response.SecurityScoreResponse;
import com.revature.security.dto.response.SecurityTrendResponse;
import com.revature.security.dashboard.PasswordStrengthDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PasswordStrengthDashboardService dashboardService;

    @GetMapping("/security-score")
    public ResponseEntity<SecurityScoreResponse> getSecurityScore(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(dashboardService.getSecurityScore(username));
    }

    @GetMapping("/password-health")
    public ResponseEntity<PasswordHealthMetricsResponse> getPasswordHealth(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(dashboardService.getPasswordHealth(username));
    }

    @GetMapping("/reused-passwords")
    public ResponseEntity<ReusedPasswordResponse> getReusedPasswords(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(dashboardService.getReusedPasswords(username));
    }

    @GetMapping("/password-age")
    public ResponseEntity<PasswordAgeResponse> getPasswordAge(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(dashboardService.getPasswordAge(username));
    }

    @GetMapping("/trends")
    public ResponseEntity<SecurityTrendResponse> getSecurityTrends(
            @RequestHeader("X-User-Name") String username,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getSecurityTrends(username, days));
    }

    @GetMapping("/passwords/weak")
    public ResponseEntity<List<com.revature.security.dto.response.VaultEntryResponse>> getWeakPasswordsList(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(dashboardService.getWeakPasswordsList(username));
    }

    @GetMapping("/passwords/old")
    public ResponseEntity<List<com.revature.security.dto.response.VaultEntryResponse>> getOldPasswordsList(@RequestHeader("X-User-Name") String username) {
        return ResponseEntity.ok(dashboardService.getOldPasswordsList(username));
    }
}
