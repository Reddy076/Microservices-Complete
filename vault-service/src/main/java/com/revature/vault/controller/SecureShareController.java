package com.revature.vault.controller;

import com.revature.vault.dto.request.CreateShareRequest;
import com.revature.vault.dto.response.ShareLinkResponse;
import com.revature.vault.dto.response.SharedPasswordResponse;
import com.revature.vault.service.sharing.SecureShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
@Tag(name = "Secure Sharing", description = "Create and manage time-limited, encrypted password shares")
public class SecureShareController {

    private final SecureShareService shareService;

    @Operation(summary = "Create a new secure share for a vault entry")
    @PostMapping
    public ResponseEntity<ShareLinkResponse> createShare(
            @Valid @RequestBody CreateShareRequest request) {
        String username = getCurrentUsername();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shareService.createShare(username, request));
    }

    @Operation(summary = "Retrieve a shared password by token (public — no auth required)")
    @GetMapping("/{token}")
    public ResponseEntity<SharedPasswordResponse> getSharedPassword(
            @PathVariable String token) {
        return ResponseEntity.ok(shareService.getSharedPassword(token));
    }

    @Operation(summary = "List all active shares created by the authenticated user")
    @GetMapping
    public ResponseEntity<List<ShareLinkResponse>> getActiveShares() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(shareService.getActiveShares(username));
    }

    @Operation(summary = "Revoke a share (owner only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ShareLinkResponse> revokeShare(@PathVariable Long id) {
        String username = getCurrentUsername();
        return ResponseEntity.ok(shareService.revokeShare(username, id));
    }

    @Operation(summary = "List shares received by the authenticated user (targeted by their email)")
    @GetMapping("/received")
    public ResponseEntity<List<ShareLinkResponse>> getReceivedShares() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(shareService.getReceivedShares(username));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

