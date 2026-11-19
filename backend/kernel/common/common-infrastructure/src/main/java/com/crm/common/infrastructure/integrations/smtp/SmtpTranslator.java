package com.crm.common.infrastructure.integrations.smtp;

import com.crm.common.application.email.EmailMessage;
import com.crm.common.application.exceptions.InternalServerErrorException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;

/** Única classe que conhece o nosso {@link EmailMessage} e o {@link MimeMessage} do Jakarta Mail. */
@RequiredArgsConstructor
class SmtpTranslator {

    private final String from;

    void write(EmailMessage message, MimeMessage mime) {
        try {
            var helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());

            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.text(), message.html());
        }

        catch (MessagingException e) {
            throw new InternalServerErrorException("falha ao montar o e-mail", e);
        }
    }
}
