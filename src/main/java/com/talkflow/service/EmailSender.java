package com.talkflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String toEmail, String verificationLink, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify Your TalkFlow Account");

        String emailText = String.format(
                "Hello,\n\n" +
                        "Welcome to TalkFlow! We're excited to have you on board.\n\n" +
                        "Your One-Time Verification Code is:\n\n    %s\n\n" +
                        "You can enter this code on the verification page, or simply click the link below:\n%s\n\n" +
                        "This code will expire in 24 hours.\n\n" +
                        "Please note: If you do not verify your account within 30 days, " +
                        "you will need to start the account creation process again.\n\n" +
                        "Thank you for joining us!\n" +
                        "— The TalkFlow Team",
                token, verificationLink
        );

        message.setText(emailText);
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset Your TalkFlow Password");

        String emailText = String.format(
                "Hello,\n\n" +
                        "We received a request to reset your TalkFlow password.\n\n" +
                        "Click the link below to reset your password:\n%s\n\n" +
                        "This link will expire in 20 minutes.\n\n" +
                        "If you didn’t request this, you can safely ignore this email.\n\n" +
                        "— The TalkFlow Security Team",
                resetLink
        );

        message.setText(emailText);
        mailSender.send(message);
    }
}
