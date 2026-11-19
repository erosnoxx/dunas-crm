package com.crm.common.infrastructure.integrations.smtp;

import com.crm.common.application.email.EmailMessage;
import com.crm.common.application.email.EmailSenderService;
import com.crm.common.application.exceptions.ExternalServiceException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;

/**
 * Envio por SMTP. Monta a mensagem pelo {@link SmtpTranslator} e entrega pelo
 * {@link SmtpClient}, repetindo só falha transitória de envio. Falha de autenticação ou de
 * montagem não melhora na segunda tentativa e sobe na hora.
 */
class SmtpEmailSenderServiceImpl implements EmailSenderService {

    private final SmtpClient client;
    private final SmtpTranslator translator;
    private final Retry retry;

    SmtpEmailSenderServiceImpl(
            SmtpClient client,
            SmtpTranslator translator,
            EmailProperties properties
    ) {
        this.client = client;
        this.translator = translator;
        this.retry = Retry.of("smtp-email", RetryConfig.custom()
                .maxAttempts(properties.maxAttempts())
                .intervalFunction(IntervalFunction.ofExponentialBackoff(properties.initialBackoff(), 2))
                .retryOnException(throwable -> throwable instanceof MailSendException)
                .build());
    }

    @Override
    public void send(EmailMessage message) {
        var mime = client.newMessage();

        translator.write(message, mime);

        try {
            Retry.decorateRunnable(retry, () -> client.send(mime)).run();
        }

        catch (MailException e) {
            throw new ExternalServiceException("falha ao enviar e-mail", e);
        }
    }
}
