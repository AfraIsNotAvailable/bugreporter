package com.group11.notificationservice.service;

import com.group11.notificationservice.dto.BanNotificationRequest;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromNumber;

    public void sendBanNotification(BanNotificationRequest request) {
        sendEmail(request);
        sendSms(request);
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

    private void sendSms(BanNotificationRequest request) {
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            System.out.println("SMS not sent: user has no phone number.");
            return;
        }

        Twilio.init(accountSid, authToken);

        Message message = Message.creator(
                new PhoneNumber(request.getPhoneNumber()),
                new PhoneNumber(fromNumber),
                "Your Bug Reporter account has been banned."
        ).create();

        System.out.println("SMS sent: " + message.getSid());
    }
}