package com.revature.vault.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications/internal/create")
    void createNotification(@RequestParam("username") String username, 
                            @RequestParam("type") String type, 
                            @RequestParam("title") String title, 
                            @RequestParam("message") String message);
}
