package com.crm.shared.domain.vos.misc;

import com.crm.common.domain.vos.BigDecimalValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Quantidade inteira e não negativa — ingressos num item de pedido, vagas de um lote.
 *
 * <p>Escala zero: não existe meio ingresso. Um valor fracionário de entrada é <b>recusado</b>,
 * não truncado, porque receber "2.5" onde se espera quantidade é defeito de quem chamou.
 */
public final class Quantity extends BigDecimalValueObject<Quantity> {

    private static final int SCALE = 0;

    /**
     * DOWN, e não HALF_UP: se um dia uma conta produzir fração, arredondar <b>para cima</b>
     * comprometeria estoque que não existe. Sobrar é seguro, faltar não.
     */
    private static final RoundingMode ROUNDING = RoundingMode.DOWN;

    private Quantity(BigDecimal value) {
        super(value);
    }

    public static Quantity of(BigDecimal value) {
        return new Quantity(value);
    }

    public static Quantity of(long value) {
        return new Quantity(BigDecimal.valueOf(value));
    }

    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }

    public Quantity add(Quantity other) {
        return plus(other);
    }

    /** Subtrair abaixo de zero é recusado: estoque negativo é sempre defeito. */
    public Quantity subtract(Quantity other) {
        return minus(other);
    }

    public int asInt() {
        return value.intValueExact();
    }

    @Override
    protected int scale() {
        return SCALE;
    }

    @Override
    protected RoundingMode roundingMode() {
        return ROUNDING;
    }

    @Override
    protected String type() {
        return "quantidade";
    }

    @Override
    protected boolean allowNegative() {
        return false;
    }

    @Override
    protected Quantity newInstance(BigDecimal value) {
        return new Quantity(value);
    }
}
