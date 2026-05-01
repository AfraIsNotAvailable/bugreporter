package com.group11.notificationservice.service;

import com.group11.notificationservice.dto.BanNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    public void sendBanNotification(BanNotificationRequest request) {
        sendEmail(request);
        sendSmsSimulation(request);
    }

    private void sendEmail(BanNotificationRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(request.getEmail());
        message.setSubject("Account banned");
        message.setText(
                "Hello " + request.getUsername() + ",\n\n" +
                        "Your Bug Reporter account has been banned.\n\n" +
                        "If you think this is a mistake, please contact support."
        );

        mailSender.send(message);
    }

    private void sendSmsSimulation(BanNotificationRequest request) {
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            System.out.println("SMS sent to " + request.getPhoneNumber()
                    + ": Your Bug Reporter account has been banned.");
        } else {
            System.out.println("SMS not sent: user has no phone number.");
        }
    }
}