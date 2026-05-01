package com.group11.notificationservice.dto;

import lombok.Data;

@Data
public class BanNotificationRequest {
    private String username;
    private String email;
    private String phoneNumber;
}