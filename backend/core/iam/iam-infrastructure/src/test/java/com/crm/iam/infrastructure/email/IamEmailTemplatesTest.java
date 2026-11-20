package com.crm.iam.infrastructure.email;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.Role;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;
import com.crm.iam.domain.vos.UserName;
import com.crm.shared.domain.vos.contact.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IamEmailTemplates")
class IamEmailTemplatesTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private static final TokenSecret SECRET = TokenSecret.of("AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde");

    private final IamEmailTemplates templates = new IamEmailTemplates(
            "https://crm.dunas.com.br/",
            Duration.ofHours(48),
            Duration.ofHours(1)
    );

    private static User user(String name) {
        return User.invite(UserName.of(name), Email.of("ana@dunas.com"), Role.COMMERCIAL, NOW);
    }

    @Test
    @DisplayName("convite: destinatário, assunto, link com o segredo e validade de 48 horas")
    void invitationCarriesRecipientSubjectLinkAndValidity() {
        var message = templates.build(user("Ana Souza"), TokenPurpose.INVITATION, SECRET);

        var link = "https://crm.dunas.com.br/convite?token=" + SECRET.getValue();

        assertThat(message.to()).isEqualTo("ana@dunas.com");
        assertThat(message.subject()).isEqualTo("Seu convite para o sistema da DUNAS");
        assertThat(message.html()).contains(link).contains("Ana Souza").contains("48 horas");
        assertThat(message.text()).contains(link).contains("Ana Souza").contains("48 horas");
    }

    @Test
    @DisplayName("redefinição: assunto, link com o segredo e validade de 1 hora")
    void passwordResetCarriesSubjectLinkAndValidity() {
        var message = templates.build(user("Ana Souza"), TokenPurpose.PASSWORD_RESET, SECRET);

        var link = "https://crm.dunas.com.br/redefinir-senha?token=" + SECRET.getValue();

        assertThat(message.subject()).isEqualTo("Redefinição de senha na DUNAS");
        assertThat(message.html()).contains(link).contains("1 hora").doesNotContain("1 horas");
        assertThat(message.text()).contains(link).contains("1 hora").doesNotContain("1 horas");
    }

    @Test
    @DisplayName("a validade vem da configuração, não do texto")
    void validityComesFromConfiguration() {
        var custom = new IamEmailTemplates("http://localhost:5173", Duration.ofHours(72), Duration.ofMinutes(30));

        assertThat(custom.build(user("Ana"), TokenPurpose.INVITATION, SECRET).text()).contains("72 horas");
        assertThat(custom.build(user("Ana"), TokenPurpose.PASSWORD_RESET, SECRET).text()).contains("30 minutos");
    }

    @Test
    @DisplayName("o nome é escapado no HTML e fica cru no texto")
    void nameIsEscapedInHtmlOnly() {
        var message = templates.build(user("<b>Ana</b> & Cia"), TokenPurpose.INVITATION, SECRET);

        assertThat(message.html()).contains("&lt;b&gt;Ana&lt;/b&gt; &amp; Cia").doesNotContain("<b>Ana");
        assertThat(message.text()).contains("<b>Ana</b> & Cia");
    }

    @Test
    @DisplayName("a barra final do appUrl não duplica no link")
    void trailingSlashIsNotDuplicated() {
        var message = templates.build(user("Ana"), TokenPurpose.INVITATION, SECRET);

        assertThat(message.text()).doesNotContain("br//convite");
    }

    @Test
    @DisplayName("appUrl em branco é recusado na construção")
    void blankAppUrlIsRejected() {
        assertThatThrownBy(() -> new IamEmailTemplates(" ", Duration.ofHours(1), Duration.ofHours(1)))
                .isInstanceOf(InternalServerErrorException.class);
    }
}
