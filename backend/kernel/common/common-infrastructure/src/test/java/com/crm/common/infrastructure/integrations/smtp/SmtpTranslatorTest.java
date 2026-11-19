package com.crm.common.infrastructure.integrations.smtp;

import com.crm.common.application.email.EmailMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmtpTranslator")
class SmtpTranslatorTest {

    @Test
    @DisplayName("monta a mensagem com remetente, destinatário, assunto e as duas versões do corpo")
    void writesMessage() throws Exception {
        var mime = new MimeMessage(Session.getInstance(new Properties()));
        var message = new EmailMessage("marina@email.com", "Confirme seu e-mail no CRM", "<p>ola</p>", "ola");

        new SmtpTranslator("CRM <no-reply@crm.local>").write(message, mime);
        mime.saveChanges();

        var raw = new ByteArrayOutputStream();
        mime.writeTo(raw);

        assertThat(mime.getFrom()[0].toString()).isEqualTo("CRM <no-reply@crm.local>");
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("marina@email.com");
        assertThat(mime.getSubject()).isEqualTo("Confirme seu e-mail no CRM");
        assertThat(raw.toString(StandardCharsets.UTF_8))
                .contains("text/plain")
                .contains("text/html")
                .contains("<p>ola</p>");
    }
}
