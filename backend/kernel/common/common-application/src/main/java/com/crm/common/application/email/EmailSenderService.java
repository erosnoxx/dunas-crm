package com.crm.common.application.email;

/**
 * Envio de e-mail. A implementação é uma integração com terceiro e segue a Anti-Corruption Layer
 * (architecture.md §4.3).
 *
 * <p>É chamado fora da thread do request, por listener assíncrono (architecture.md §5.1). A
 * implementação repete falha transitória; esgotadas as tentativas, lança
 * {@code ExternalServiceException}.
 */
public interface EmailSenderService {

    void send(EmailMessage message);
}
