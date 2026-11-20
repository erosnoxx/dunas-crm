package com.crm.common.application.email;


import com.crm.common.application.exceptions.InternalServerErrorException;

/**
 * Um e-mail pronto para envio. O conteúdo é montado por quem envia; o contrato só entrega.
 *
 * <p>O corpo pode trazer um link com segredo (convite, redefinição de senha): o {@link #toString()}
 * mostra só destinatário e assunto.
 *
 * @param text versão em texto puro, para clientes que não mostram HTML
 */
public record EmailMessage(
        String to,
        String subject,
        String html,
        String text
) {
    public EmailMessage {
        if (to == null || to.isBlank())
            throw new InternalServerErrorException("e-mail sem destinatário");

        if (subject == null || subject.isBlank())
            throw new InternalServerErrorException("e-mail sem assunto");

        if (html == null || text == null)
            throw new InternalServerErrorException("e-mail sem corpo");
    }

    @Override
    public String toString() {
        return "EmailMessage[to=" + to + ", subject=" + subject + ", html=***, text=***]";
    }
}
