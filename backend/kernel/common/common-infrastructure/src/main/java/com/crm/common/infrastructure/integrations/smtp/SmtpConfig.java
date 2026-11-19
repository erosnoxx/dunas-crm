package com.crm.common.infrastructure.integrations.smtp;

import com.crm.common.application.email.EmailSenderService;
import com.crm.common.application.exceptions.InternalServerErrorException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Mora dentro da integração porque conhece o {@link JavaMailSender}: fora de
 * {@code ..infrastructure.integrations..}, o {@code AntiCorruptionLayerRulesTest} recusa.
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class SmtpConfig {

    @Bean
    public EmailSenderService emailSenderService(JavaMailSender mailSender, EmailProperties properties) {
        if (properties.from() == null || properties.from().isBlank())
            throw new InternalServerErrorException("crm.email.from não configurado");

        return new SmtpEmailSenderServiceImpl(
                new SmtpClient(mailSender),
                new SmtpTranslator(properties.from()),
                properties
        );
    }
}
