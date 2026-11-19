package com.crm.common.domain.vos;


import com.crm.common.domain.exceptions.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Base de todo Value Object numérico.
 *
 * <p>O parâmetro de tipo é o próprio subtipo ({@code class Money extends
 * BigDecimalValueObject<Money>}). É o que faz as operações devolverem o VO, e não um
 * {@code BigDecimal} cru — um valor solto escaparia sem validação, sem escala garantida e sem
 * tipo, e somar dinheiro com quantidade compilaria.
 *
 * <h2>Entrada estrita, aritmética arredondada</h2>
 *
 * <p>São duas regras diferentes de propósito:
 *
 * <ul>
 *   <li><b>Entrada</b> ({@link #validate}): um valor com mais casas decimais do que
 *       {@link #scale()} é <b>recusado</b>, nunca arredondado. Preço com três casas vindo de uma
 *       API ou do banco é defeito, e arredondar em silêncio o dinheiro de alguém esconde o
 *       defeito.
 *   <li><b>Aritmética</b> ({@link #roundingMode()}): aqui arredondar é obrigatório. Taxa sobre
 *       preço, rateio, divisão em parcelas — nenhuma dessas contas fecha exata, e recusá-las
 *       tornaria o VO inútil.
 * </ul>
 *
 * <p>Foi exatamente aqui que o desenho anterior falhava: {@link #roundingMode()} servia aos dois
 * papéis, então {@code UNNECESSARY} (correto para entrada) fazia toda multiplicação por
 * percentual lançar {@code ArithmeticException} nativa.
 *
 * <p><b>Atenção ao implementar:</b> {@link #scale()}, {@link #roundingMode()}, {@link #type()} e
 * {@link #allowNegative()} são chamados de dentro do construtor, antes de os campos da subclasse
 * existirem. Devolva constantes, nunca o valor de um campo — um campo retornaria {@code null}
 * ou zero.
 *
 * @param <T> o próprio subtipo
 */
public abstract class BigDecimalValueObject<T extends BigDecimalValueObject<T>> extends ValueObject<BigDecimal> {

    protected BigDecimalValueObject(BigDecimal value) {
        super(value);
    }

    /** Casas decimais do tipo. Constante, nunca um campo — ver a nota da classe. */
    protected abstract int scale();

    /** Arredondamento usado na <b>aritmética</b>. Não participa da validação de entrada. */
    protected abstract RoundingMode roundingMode();

    /** Nome do tipo nas mensagens de erro: pt-BR, minúsculo (ex. "valor monetário"). */
    protected abstract String type();

    protected abstract boolean allowNegative();

    /** Constrói uma nova instância do subtipo. É o que permite as operações devolverem {@code T}. */
    protected abstract T newInstance(BigDecimal value);

    @Override
    protected BigDecimal validate(BigDecimal value) {
        if (value == null)
            throw new ValidationException(type() + " não pode ser nulo");

        if (!allowNegative() && value.signum() < 0)
            throw new ValidationException(type() + " não pode ser negativo");

        // stripTrailingZeros antes de medir a escala: "10.00" tem escala 2 mas representa um
        // inteiro, e recusá-lo seria absurdo. Só o excesso de casas *significativas* é erro.
        if (value.stripTrailingZeros().scale() > scale())
            throw new ValidationException(
                    type() + " deve possuir no máximo " + scale() + " casas decimais"
            );

        // seguro: a checagem acima já garantiu que não há precisão a perder
        return value.setScale(scale(), roundingMode());
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    public boolean isGreaterThan(T other) {
        return value.compareTo(requireOther(other).value) > 0;
    }

    public boolean isLessThan(T other) {
        return value.compareTo(requireOther(other).value) < 0;
    }

    public boolean isGreaterThanOrEqualTo(T other) {
        return value.compareTo(requireOther(other).value) >= 0;
    }

    public boolean isLessThanOrEqualTo(T other) {
        return value.compareTo(requireOther(other).value) <= 0;
    }

    // --- aritmética ---------------------------------------------------------------------
    //
    // protected de propósito: a base concentra as guardas e o arredondamento, e cada VO publica
    // só as operações que fazem sentido no domínio dele, com o nome do domínio. Percentage não
    // deveria expor add; Money não deveria expor "multiplicar por um decimal qualquer".
    //
    // Não existe divide aqui, e a ausência é deliberada: dividir dinheiro ingenuamente perde
    // centavo (10,00 / 3 = 3,33, e 3,33 x 3 = 9,99). Rateio é uma operação de domínio que
    // precisa decidir para quem vai a sobra, então pertence ao VO que a entender, não à base.

    protected T plus(T other) {
        return newInstance(value.add(requireOther(other).value));
    }

    /**
     * Subtrair pode violar {@link #allowNegative()} — nesse caso o próprio construtor recusa, e
     * o resultado é uma {@link ValidationException}, não um valor negativo silencioso.
     */
    protected T minus(T other) {
        return newInstance(value.subtract(requireOther(other).value));
    }

    /** Arredonda com {@link #roundingMode()} antes de construir: taxa sobre preço raramente é exata. */
    protected T scaledBy(BigDecimal factor) {
        if (factor == null)
            throw new ValidationException("fator não pode ser nulo");

        return newInstance(value.multiply(factor).setScale(scale(), roundingMode()));
    }

    private T requireOther(T other) {
        if (other == null)
            throw new ValidationException(type() + " não pode ser comparado com nulo");

        return other;
    }
}
