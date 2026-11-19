package com.crm.common.infrastructure.integrations.smtp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ajustes do envio de e-mail.
 *
 * <pre>
 * crm.email.from=CRM &lt;no-reply@exemplo.com&gt;
 * crm.email.max-attempts=3
 * crm.email.initial-backoff=1s
 * </pre>
 *
 * @param from           remetente; obrigatório, sem default
 * @param maxAttempts    tentativas no total, contando a primeira
 * @param initialBackoff espera antes da segunda tentativa; dobra a cada tentativa seguinte
 */
@ConfigurationProperties(prefix = "crm.email")
public record EmailProperties(
        String from,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("1s") Duration initialBackoff
) {
}
