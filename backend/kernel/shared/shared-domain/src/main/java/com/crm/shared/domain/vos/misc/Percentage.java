package com.crm.shared.domain.vos.misc;

import com.crm.common.domain.exceptions.ValidationException;
import com.crm.common.domain.vos.BigDecimalValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Percentual, guardado na forma que se lê: {@code 7.50} é sete e meio por cento.
 *
 * <p>Guardar o número lido, e não a fração (0.075), é deliberado: é assim que o valor aparece na
 * configuração, no banco e na conversa com o organizador. A conversão para fração acontece só na
 * hora de calcular, em {@link #asFraction()}.
 *
 * <p><b>Limitado a 0–100.</b> O uso no produto é taxa da plataforma e desconto, e nos dois casos
 * passar de 100% é erro — normalmente confundir fração com percentual. Recusar aqui faz o erro
 * aparecer onde nasceu, e não num valor cobrado errado.
 *
 * <p>Não expõe soma nem subtração: somar percentuais quase sempre indica que o cálculo deveria
 * ser feito sobre o dinheiro, não sobre as taxas.
 */
public final class Percentage extends BigDecimalValueObject<Percentage> {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal MAX = BigDecimal.valueOf(100);

    private Percentage(BigDecimal value) {
        super(value);

        if (value.compareTo(MAX) > 0)
            throw new ValidationException("percentual não pode ser maior que 100");
    }

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    public static Percentage zero() {
        return new Percentage(BigDecimal.ZERO);
    }

    /** {@code 7.50} vira {@code 0.0750}. Conversão exata: só desloca a vírgula, não arredonda. */
    public BigDecimal asFraction() {
        return value.movePointLeft(2);
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
        return "percentual";
    }

    @Override
    protected boolean allowNegative() {
        return false;
    }

    @Override
    protected Percentage newInstance(BigDecimal value) {
        return new Percentage(value);
    }
}
