package com.group11.notificationservice.controller;

import com.group11.notificationservice.dto.BanNotificationRequest;
import com.group11.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/ban")
    public ResponseEntity<String> sendBanNotification(@RequestBody BanNotificationRequest request) {
        notificationService.sendBanNotification(request);
        return ResponseEntity.ok("Ban notification sent successfully.");
    }
}