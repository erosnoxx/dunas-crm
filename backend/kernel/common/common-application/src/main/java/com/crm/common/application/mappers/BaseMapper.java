package com.crm.common.application.mappers;


import com.crm.common.domain.vos.ValueObject;

/**
 * Base dos mappers entre domínio e schemas (request/response) da application.
 *
 * <p>{@link #fromVO(ValueObject)} desembrulha um Value Object e tolera {@code null}, para o mapeamento
 * de campos opcionais não precisar checar.
 */
public interface BaseMapper {
    default <T> T fromVO(ValueObject<T> vo) {
        return vo == null ? null : vo.getValue();
    }
}
