package com.crm.common.application.storage;

import java.time.Instant;

/**
 * O que o navegador precisa para enviar um arquivo: faça {@code PUT} em {@code url} com o
 * cabeçalho {@code Content-Type: contentType} e o arquivo no corpo, antes de {@code expiresAt}.
 */
public record UploadTicket(String url, String contentType, Instant expiresAt) {
}
