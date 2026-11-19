package com.crm.common.application.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache de leitura. Consumido pelas queries de {@code <bc>-application}.
 *
 * <h2>O banco é a fonte de verdade — isto é só otimização</h2>
 *
 * <p>Nenhuma decisão de negócio depende do cache. Lock de reserva de ingresso é por constraint
 * ou {@code FOR UPDATE} no banco, <b>nunca</b> por cache.
 *
 * <p>Consequência direta no contrato: <b>a implementação nunca propaga falha de cache</b>. Se o
 * Redis estiver fora, {@link #getOrLoad} cai no loader e a aplicação continua servindo — mais
 * devagar, não quebrada. Um {@code put} ou {@code evict} que falha é registrado em log e
 * engolido. Nunca escreva código que dependa de uma exception vinda daqui.
 *
 * <h2>Invalidação é por evento; TTL é rede de segurança</h2>
 *
 * <p>O mecanismo principal é chamar {@link #evict} (ou {@link #evictByPrefix}) no listener do
 * evento que tornou o dado obsoleto. O TTL curto existe só para limitar o dano quando alguém
 * esquecer de invalidar — não é a estratégia.
 */
public interface Cache {

    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Devolve o valor cacheado ou executa o loader e cacheia o resultado.
     *
     * <p>É a forma preferida de consumo: uma chamada, sem ramo de "está no cache?" espalhado
     * pelas queries. O loader é sempre executado quando há falha de cache.
     *
     * <p>{@code null} devolvido pelo loader não é cacheado.
     */
    <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader);

    void put(String key, Object value, Duration ttl);

    void evict(String key);

    /**
     * Invalida todas as chaves sob um prefixo — usado quando um evento invalida uma família de
     * entradas (ex. todas as projeções de um evento).
     *
     * <p>A implementação varre incrementalmente; nunca com um comando que bloqueie o servidor
     * de cache inteiro.
     */
    void evictByPrefix(String prefix);
}
