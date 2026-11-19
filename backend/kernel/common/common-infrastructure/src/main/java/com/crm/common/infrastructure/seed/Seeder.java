package com.crm.common.infrastructure.seed;

import java.util.List;

/**
 * Carga inicial de dados de um BC, lida de um JSON no classpath.
 *
 * @param <T> record em que cada item do JSON é lido
 */
public interface Seeder<T> {
    void seed(List<T> data);
    String resourcePath();
    Class<T> recordType();
}
