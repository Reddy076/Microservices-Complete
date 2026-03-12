package com.revature.vault.client;

import com.revature.vault.dto.response.AuditLogResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "security-service")
public interface SecurityClient {

    @PostMapping("/api/security/internal/audit")
    void logAction(@RequestParam("username") String username, 
                   @RequestParam("action") String action, 
                   @RequestParam("details") String details);

    @PostMapping("/api/security/internal/analyze")
    void analyzePasswordEntry(@RequestBody VaultEntryPayload entry);

    @GetMapping("/api/security/audit-logs")
    List<AuditLogResponse> getAuditLogs(@RequestHeader("X-User-Name") String username);

    class VaultEntryPayload {
        private Long id;
        private String title;
        private String username;
        private String password;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
