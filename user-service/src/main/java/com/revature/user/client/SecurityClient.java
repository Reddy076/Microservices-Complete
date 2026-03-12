package com.revature.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "security-service")
public interface SecurityClient {

    @PostMapping("/internal/audit")
    void logAudit(@RequestParam("username") String username, 
                  @RequestParam("action") String action, 
                  @RequestParam("details") String details, 
                  @RequestParam("ipAddress") String ipAddress);

    @PostMapping("/internal/alerts")
    void createAlert(@RequestParam("username") String username, 
                     @RequestParam("type") String type, 
                     @RequestParam("title") String title, 
                     @RequestParam("message") String message, 
                     @RequestParam("severity") String severity);
}
