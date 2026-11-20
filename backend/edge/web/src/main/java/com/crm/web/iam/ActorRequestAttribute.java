package com.crm.web.iam;

/**
 * Nome do atributo de requisição onde a cadeia de segurança do {@code boot} deixa o
 * {@link com.crm.iam.application.Actor} da sessão autenticada.
 *
 * <p>É o único elo entre a autenticação e os controllers: o {@link ActorArgumentResolver} lê
 * daqui e nenhum controller toca em cookie, token ou cabeçalho de credencial.
 */
public final class ActorRequestAttribute {

    /** Chave do atributo; o valor guardado é um {@code Actor}. */
    public static final String NAME = "com.crm.iam.actor";

    private ActorRequestAttribute() {
    }
}
