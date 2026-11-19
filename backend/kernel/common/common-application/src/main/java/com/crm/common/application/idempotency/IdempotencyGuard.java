package com.crm.common.application.idempotency;

import java.util.function.Supplier;

/**
 * Garante que uma operação com efeito colateral rode no máximo uma vez por chave, mesmo sob
 * repetição — retry de rede, duplo clique, resubmit do navegador. Mesmo padrão público de
 * Stripe/Adyen/PayPal (header {@code Idempotency-Key}): emitir uma vez, resgatar uma vez,
 * `ConflictException` se reaproveitado.
 *
 * <p>Usado sobretudo por classes de {@code orchestration} (o substituto do antigo `Gateway` —
 * ver docs/architecture.md §5.4), mas não é exclusivo delas: qualquer entrada que pode chegar
 * duplicada pela natureza do transporte (um webhook, por exemplo) usa o mesmo contrato.
 *
 * <p><b>Não decide nada de negócio.</b> Não sabe o que {@code operation} faz, não decide
 * compensação, não abre transação em volta de {@code operation} — só garante "isto já rodou
 * ou não". A sequência de passos e a decisão de compensação são de quem chama, escritas à mão
 * (ver docs/architecture.md §5.4 e o porquê de não existir um runner genérico aqui).
 */
public interface IdempotencyGuard {

    /**
     * @param key               identifica a execução. Vem de quem chama (header do cliente,
     *                          ou derivado de algo estável do fluxo, como um id de carrinho) —
     *                          este contrato não decide de onde ela vem.
     * @param requestFingerprint qualquer objeto que represente o pedido (tipicamente o próprio
     *                          request DTO). Serializado e hasheado internamente: se a mesma
     *                          chave chegar de novo com um fingerprint diferente, é uso
     *                          incorreto da chave, não repetição legítima.
     * @param resultType        classe do retorno — necessária para desserializar o resultado
     *                          guardado numa repetição, já que genéricos Java apagam o tipo em
     *                          runtime.
     * @param operation         a operação em si. Roda **fora** de qualquer transação deste
     *                          guard — cada use case que ela chamar abre e fecha a própria
     *                          transação normalmente, como se o guard não existisse.
     * @return o resultado de {@code operation}, novo ou repetido de uma execução anterior
     */
    <T> T runOnce(String key, Object requestFingerprint, Class<T> resultType, Supplier<T> operation);
}
