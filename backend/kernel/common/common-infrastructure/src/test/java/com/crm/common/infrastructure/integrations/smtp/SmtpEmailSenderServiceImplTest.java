package com.crm.common.infrastructure.integrations.smtp;

import com.crm.common.application.email.EmailMessage;
import com.crm.common.application.exceptions.ExternalServiceException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SmtpEmailSenderServiceImpl")
class SmtpEmailSenderServiceImplTest {

    private static final String FROM = "CRM <no-reply@crm.local>";

    private static final EmailMessage MESSAGE =
            new EmailMessage("marina@email.com", "Confirme seu e-mail no CRM", "<p>ola</p>", "ola");

    private JavaMailSender mailSender;
    private SmtpEmailSenderServiceImpl service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);

        when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));

        service = new SmtpEmailSenderServiceImpl(
                new SmtpClient(mailSender),
                new SmtpTranslator(FROM),
                new EmailProperties(FROM, 3, Duration.ofMillis(1))
        );
    }

    @Test
    @DisplayName("envia uma vez quando o servidor responde")
    void sendsOnce() {
        service.send(MESSAGE);

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("repete falha transitória até conseguir")
    void retriesTransientFailure() {
        doThrow(new MailSendException("servidor fora"))
                .doThrow(new MailSendException("servidor fora"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        service.send(MESSAGE);

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("desiste depois das tentativas e lança ExternalServiceException")
    void givesUpAfterMaxAttempts() {
        doThrow(new MailSendException("servidor fora")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.send(MESSAGE))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("falha ao enviar e-mail");

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("não repete falha de autenticação, que não melhora sozinha")
    void doesNotRetryAuthenticationFailure() {
        doThrow(new MailAuthenticationException("senha de app inválida")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.send(MESSAGE))
                .isInstanceOf(ExternalServiceException.class);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
