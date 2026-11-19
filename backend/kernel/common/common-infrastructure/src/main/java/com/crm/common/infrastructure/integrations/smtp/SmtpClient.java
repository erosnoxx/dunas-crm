package com.crm.common.infrastructure.integrations.smtp;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;

/** Fala com o servidor SMTP. Só conhece o modelo do Jakarta Mail. */
@RequiredArgsConstructor
class SmtpClient {

    private final JavaMailSender mailSender;

    MimeMessage newMessage() {
        return mailSender.createMimeMessage();
    }

    void send(MimeMessage message) {
        mailSender.send(message);
    }
}
