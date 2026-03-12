package com.revature.security.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@FeignClient(name = "notification-service")
public interface NotificationClient {
    @PostMapping("/internal/notify")
    void sendNotification(@RequestBody NotificationRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NotificationRequest {
        private String username;
        private String type;
        private String title;
        private String message;
    }
}
