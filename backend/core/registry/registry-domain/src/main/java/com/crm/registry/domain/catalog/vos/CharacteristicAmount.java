package com.crm.registry.domain.catalog.vos;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.BigDecimalValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor de uma característica de um modelo (ex. 5 kW, 12000 BTU/h): maior que zero, até 3 casas
 * decimais e até 9 dígitos inteiros, o que cabe em {@code numeric(12,3)}.
 *
 * <p>Toda recusa sai com o código {@code invalid_amount} e a mesma mensagem, inclusive texto não
 * numérico e nulo, para a tela tratar um só caso. Zeros à direita não contam como casa decimal
 * ({@code "1.2000"} vale {@code 1.200}), a mesma regra da base.
 */
public final class CharacteristicAmount extends BigDecimalValueObject<CharacteristicAmount> {

    private static final String CODE = "invalid_amount";
    private static final String MESSAGE = "o valor precisa ser maior que zero, com até 3 casas decimais";
    private static final int SCALE = 3;
    private static final int MAX_INTEGER_DIGITS = 9;

    private CharacteristicAmount(BigDecimal value) {
        super(value);
    }

    public static CharacteristicAmount of(BigDecimal value) {
        if (value == null)
            throw invalid();

        return new CharacteristicAmount(value);
    }

    public static CharacteristicAmount of(String value) {
        if (value == null)
            throw invalid();

        try {
            return new CharacteristicAmount(new BigDecimal(value.trim()));
        }
        catch (NumberFormatException e) {
            throw invalid();
        }
    }

    @Override
    protected BigDecimal validate(BigDecimal value) {
        var stripped = value.stripTrailingZeros();

        if (value.signum() <= 0 || stripped.scale() > SCALE || stripped.precision() - stripped.scale() > MAX_INTEGER_DIGITS)
            throw invalid();

        return super.validate(value);
    }

    private static ValidationException invalid() {
        return new ValidationException(CODE, MESSAGE);
    }

    @Override
    protected int scale() {
        return SCALE;
    }

    @Override
    protected RoundingMode roundingMode() {
        return RoundingMode.HALF_UP;
    }

    @Override
    protected String type() {
        return "valor da característica";
    }

    @Override
    protected boolean allowNegative() {
        return false;
    }

    @Override
    protected CharacteristicAmount newInstance(BigDecimal value) {
        return new CharacteristicAmount(value);
    }
}
