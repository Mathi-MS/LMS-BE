package com.live.locationtracker.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendOtp(String email, String otp) {
        logger.info("Generating OTP email for: {}", email);
        String subject = "Your OTP for Signup";
        String body = "Your OTP is: " + otp + ". It is valid for 5 minutes.";
        sendEmail(email, subject, body);
    }

    @Async
    public void sendResetLink(String email, String link) {
        logger.info("Generating password reset email for: {}", email);
        String subject = "Password Reset Request";
        String body = "Click the following link to reset your password: " + link;
        sendEmail(email, subject, body);
    }
}
