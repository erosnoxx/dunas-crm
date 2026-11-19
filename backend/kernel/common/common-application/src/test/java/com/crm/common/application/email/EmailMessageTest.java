package com.crm.common.application.email;

import com.crm.common.application.exceptions.InternalServerErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EmailMessage")
class EmailMessageTest {

    @Test
    @DisplayName("guarda destinatário, assunto e as duas versões do corpo")
    void keepsFields() {
        var message = new EmailMessage("marina@email.com", "Confirme seu e-mail no CRM", "<p>olá</p>", "olá");

        assertEquals("marina@email.com", message.to());
        assertEquals("olá", message.text());
    }

    @Test
    @DisplayName("recusa e-mail sem destinatário, sem assunto ou sem corpo")
    void rejectsMissingParts() {
        assertThrows(InternalServerErrorException.class, () -> new EmailMessage(" ", "assunto", "<p>x</p>", "x"));
        assertThrows(InternalServerErrorException.class, () -> new EmailMessage("a@b.com", "", "<p>x</p>", "x"));
        assertThrows(InternalServerErrorException.class, () -> new EmailMessage("a@b.com", "assunto", null, "x"));
    }
}
