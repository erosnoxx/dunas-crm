package com.crm.iam.infrastructure.email;

import com.crm.common.application.email.EmailMessage;
import com.crm.common.application.exceptions.InternalServerErrorException;
import com.crm.iam.domain.entities.User;
import com.crm.iam.domain.vos.TokenPurpose;
import com.crm.iam.domain.vos.TokenSecret;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Monta os e-mails do IAM (convite e redefinição de senha) em HTML e texto puro.
 *
 * <p>O texto é curto e direto, no tom da DUNAS. A validade citada vem da configuração, para o e-mail
 * nunca divergir do prazo real do link. O segredo só existe aqui dentro do link: nada o devolve a
 * quem chama, e nenhum log o recebe.
 */
public class IamEmailTemplates {

    private final String appUrl;
    private final Duration invitationValidity;
    private final Duration passwordResetValidity;

    /**
     * @param appUrl                endereço do frontend; a barra final é ignorada
     * @param invitationValidity    validade do link de convite
     * @param passwordResetValidity validade do link de redefinição
     */
    public IamEmailTemplates(
            String appUrl,
            Duration invitationValidity,
            Duration passwordResetValidity
    ) {
        if (appUrl == null || appUrl.isBlank())
            throw new InternalServerErrorException("configuração do iam sem o endereço do frontend (crm.iam.app-url)");

        this.appUrl = appUrl.strip().replaceAll("/+$", "");
        this.invitationValidity = invitationValidity;
        this.passwordResetValidity = passwordResetValidity;
    }

    public EmailMessage build(
            User user,
            TokenPurpose purpose,
            TokenSecret secret
    ) {
        var name = user.getName().getValue();
        var to = user.getEmail().getValue();

        return switch (purpose) {
            case INVITATION -> compose(
                    to,
                    "Seu convite para o sistema da DUNAS",
                    name,
                    "Você recebeu um convite para usar o sistema da DUNAS. Crie a sua senha pelo link abaixo.",
                    "Criar senha",
                    link("/convite", secret),
                    "Se o link vencer, peça um novo convite a um administrador.",
                    invitationValidity
            );
            case PASSWORD_RESET -> compose(
                    to,
                    "Redefinição de senha na DUNAS",
                    name,
                    "Recebemos um pedido para redefinir a sua senha. Crie uma nova senha pelo link abaixo.",
                    "Redefinir senha",
                    link("/redefinir-senha", secret),
                    "Se você não pediu a redefinição, ignore este e-mail: a sua senha continua a mesma.",
                    passwordResetValidity
            );
        };
    }

    private EmailMessage compose(
            String to,
            String subject,
            String name,
            String intro,
            String action,
            String link,
            String closing,
            Duration validity
    ) {
        var validityText = "O link vale por " + describe(validity) + " e só pode ser usado uma vez.";

        var text = "Olá, " + name + ".\n\n"
                + intro + "\n\n"
                + link + "\n\n"
                + validityText + " " + closing + "\n";

        var html = "<p>Olá, " + escape(name) + ".</p>\n"
                + "<p>" + escape(intro) + "</p>\n"
                + "<p><a href=\"" + escape(link) + "\">" + escape(action) + "</a></p>\n"
                + "<p>Se o link não abrir, copie este endereço no navegador:<br>" + escape(link) + "</p>\n"
                + "<p>" + escape(validityText) + " " + escape(closing) + "</p>\n";

        return new EmailMessage(to, subject, html, text);
    }

    /** O segredo é base64 url-safe, que já é seguro em URL; codifica mesmo assim, por não depender disso. */
    private String link(
            String path,
            TokenSecret secret
    ) {
        return appUrl + path + "?token=" + URLEncoder.encode(secret.getValue(), StandardCharsets.UTF_8);
    }

    private static String describe(Duration validity) {
        var minutes = validity.toMinutes();

        if (minutes % 60 != 0)
            return minutes + (minutes == 1 ? " minuto" : " minutos");

        var hours = minutes / 60;

        return hours + (hours == 1 ? " hora" : " horas");
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
