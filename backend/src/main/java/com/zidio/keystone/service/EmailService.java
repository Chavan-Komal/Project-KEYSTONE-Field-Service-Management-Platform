package com.zidio.keystone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional email. Spring Boot only creates a {@link JavaMailSender}
 * when {@code spring.mail.host} is configured — so this service takes it as an
 * {@link ObjectProvider} and degrades gracefully: with no mail server it just
 * logs the reset link at INFO (enough to demo the flow, and the link is also
 * returned in the API response when {@code keystone.auth.expose-reset-token=true}).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${keystone.mail.from:no-reply@keystone.dev}")
    private String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendPasswordReset(String toEmail, String resetUrl) {
        String subject = "Reset your KEYSTONE password";
        String body = """
            We received a request to reset the password for your KEYSTONE account.

            Open this link to choose a new password (valid for 30 minutes):
            %s

            If you didn't request this, you can safely ignore this email.
            """.formatted(resetUrl);

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.info("[password-reset] no mail server configured — reset link for {}: {}", toEmail, resetUrl);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("[password-reset] reset email sent to {}", toEmail);
        } catch (Exception ex) {
            // Never surface mail-server problems to the caller — the token is
            // still valid and recoverable from the log line above.
            log.warn("[password-reset] failed to send reset email to {} ({}); link: {}",
                toEmail, ex.getMessage(), resetUrl);
        }
    }
}
