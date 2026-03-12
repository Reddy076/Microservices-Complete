package com.revature.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/internal/by-username/{username}")
    UserVaultDetails getUserDetailsByUsername(@PathVariable("username") String username);

    @GetMapping("/api/users/internal/by-id/{id}")
    UserVaultDetails getUserDetailsById(@PathVariable("id") Long id);

    // Inner class to map response
    class UserVaultDetails {
        private Long id;
        private String username;
        private String masterPasswordHash;
        private String salt;
        private boolean is2faEnabled;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getMasterPasswordHash() { return masterPasswordHash; }
        public void setMasterPasswordHash(String masterPasswordHash) { this.masterPasswordHash = masterPasswordHash; }
        public String getSalt() { return salt; }
        public void setSalt(String salt) { this.salt = salt; }
        public boolean is2faEnabled() { return is2faEnabled; }
        public void set2faEnabled(boolean is2faEnabled) { this.is2faEnabled = is2faEnabled; }
        private boolean readOnlyMode;
        public boolean isReadOnlyMode() { return readOnlyMode; }
        public void setReadOnlyMode(boolean readOnlyMode) { this.readOnlyMode = readOnlyMode; }
    }
}


