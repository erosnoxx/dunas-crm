package com.crm.common.application.exceptions;

/**
 * Outra transação gravou o mesmo aggregate depois da leitura, e a versão lida ficou velha
 * (docs/architecture.md §8). A requisição é recusada para a pessoa tentar de novo; o código é
 * contrato com o frontend (§7.3).
 */
public class ConcurrentUpdateException extends ConflictException {

    public static final String CODE = "concurrent_update";
    public static final String MESSAGE = "o registro foi alterado ao mesmo tempo por outra operação; tente de novo";

    public ConcurrentUpdateException() {
        super(CODE, MESSAGE);
    }

    public ConcurrentUpdateException(Throwable cause) {
        super(CODE, MESSAGE, cause);
    }
}
