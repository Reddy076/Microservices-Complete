package com.revature.security.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.Data;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/internal/users/{username}")
    UserVaultDetails getUserDetailsByUsername(@PathVariable("username") String username);

    @GetMapping("/internal/users/email/{email}")
    UserVaultDetails getUserDetailsByEmail(@PathVariable("email") String email);

    @Data
    class UserVaultDetails {
        private Long id;
        private String username;
        private String email;
        private java.time.LocalDateTime createdAt;
    }
}
