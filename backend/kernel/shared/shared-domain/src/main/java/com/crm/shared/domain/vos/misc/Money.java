package com.crm.shared.domain.vos.misc;

import com.crm.common.domain.vos.BigDecimalValueObject;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetário em reais.
 *
 * <p>Mora em {@code shared-domain} porque mais de um bounded context precisa dele: preço de
 * ingresso em {@code ticketing}, total de pedido em {@code ordering}, cobrança em {@code payment}.
 *
 * <p><b>Não aceita negativo.</b> Estorno e desconto são modelados como operação sobre um valor
 * positivo, não como dinheiro negativo — um total de pedido negativo é sempre defeito, e recusar
 * na construção faz o defeito aparecer onde ele nasceu.
 *
 * <p><b>Arredondamento {@code HALF_UP}</b>, o arredondamento comercial usual, aplicado só na
 * aritmética. Um valor de entrada com mais de duas casas é recusado, não arredondado — ver
 * {@link BigDecimalValueObject}.
 */
public final class Money extends BigDecimalValueObject<Money> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money(BigDecimal value) {
        super(value);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    /** Ponto de partida de qualquer acumulação (total de pedido, soma de itens). */
    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return plus(other);
    }

    /** Subtrair abaixo de zero é recusado — ver a nota sobre negativo na classe. */
    public Money subtract(Money other) {
        return minus(other);
    }

    /** Total de uma linha de pedido: preço unitário vezes quantidade. Exato, sem arredondamento. */
    public Money times(Quantity quantity) {
        return scaledBy(quantity.getValue());
    }

    /**
     * Aplica uma taxa ou desconto. É <b>aqui</b> que o arredondamento comercial acontece: taxa
     * sobre preço quase nunca fecha exata.
     */
    public Money applyRate(Percentage rate) {
        return scaledBy(rate.asFraction());
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
        return "valor monetário";
    }

    @Override
    protected boolean allowNegative() {
        return false;
    }

    @Override
    protected Money newInstance(BigDecimal value) {
        return new Money(value);
    }
}
