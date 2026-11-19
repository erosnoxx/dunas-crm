package com.crm.common.infrastructure.seed;

import java.util.List;

/**
 * Carga inicial de dados de um BC, lida de um JSON no classpath.
 *
 * <p>O seeder roda a cada boot, inclusive em produção: por isso ele mesmo diz se já foi
 * semeado, e o {@link DatabaseSeeder} só o chama quando ainda não foi.
 *
 * @param <T> record em que cada item do JSON é lido
 */
public interface Seeder<T> {

    /** Diz se a carga deste seeder já está no banco; se estiver, o {@link #seed} não roda. */
    boolean isAlreadySeeded();

    /** Grava a carga; roda numa transação própria, desfeita por inteiro se lançar. */
    void seed(List<T> data);

    String resourcePath();

    Class<T> recordType();
}
