-- Correlação do evento com a requisição (ou job, ou mensagem) que o originou (D39).
--
-- O id é capturado quando o evento é gravado, na mesma transação do aggregate, e viaja no
-- cabeçalho da mensagem até o consumidor. Fica nulo nas linhas anteriores a esta migration, e o
-- despacho dessas linhas gera um id novo na hora.
--
-- 64 caracteres é o limite que CorrelationContext aceita.

ALTER TABLE outbox_event
    ADD COLUMN correlation_id VARCHAR(64);
