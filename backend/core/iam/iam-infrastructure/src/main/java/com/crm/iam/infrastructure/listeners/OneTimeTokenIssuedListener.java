package com.crm.iam.infrastructure.listeners;

import com.crm.common.application.email.EmailSenderService;
import com.crm.common.infrastructure.async.AsyncConfig;
import com.crm.iam.application.contracts.repositories.UserRepository;
import com.crm.iam.domain.events.OneTimeTokenIssued;
import com.crm.iam.infrastructure.email.IamEmailTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * Envia por e-mail o link de convite ou de redefinição de senha, depois do commit e fora da thread
 * do request (ADR-7).
 *
 * <p>O {@link OneTimeTokenIssued} carrega o segredo do link, e este listener é o único que o lê:
 * ele só entra no e-mail. Nunca vai a log, a mensagem de exceção ou serialização. Falha de envio
 * não propaga: o convite já commitou, e o Administrador reenvia (ou quem esqueceu a senha pede de
 * novo). Por isso o log da falha traz só o usuário, a finalidade e a mensagem da exception, que por
 * regra não carrega valor sensível.
 */
@Slf4j
@RequiredArgsConstructor
public class OneTimeTokenIssuedListener {

    private final UserRepository userRepository;
    private final EmailSenderService emailSender;
    private final IamEmailTemplates templates;

    @Async(AsyncConfig.SIDE_EFFECT_EXECUTOR)
    @EventListener
    public void on(OneTimeTokenIssued event) {
        try {
            var user = userRepository.findById(event.userId()).orElse(null);

            if (user == null) {
                log.warn("e-mail de {} não enviado: usuário {} não existe mais", event.purpose(), event.userId());
                return;
            }

            emailSender.send(templates.build(user, event.purpose(), event.secret()));
        }
        catch (RuntimeException e) {
            log.error(
                    "falha ao enviar o e-mail de {} ao usuário {}: {}",
                    event.purpose(),
                    event.userId(),
                    e.getMessage()
            );
        }
    }
}
