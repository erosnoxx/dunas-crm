package com.crm.common.domain.vos;


import com.crm.common.domain.exceptions.ValidationException;

/**
 * Base de todo Value Object de texto.
 *
 * <p>Lança {@link ValidationException} (e não {@code DomainException}) porque recusar entrada
 * malformada é 400, não 422 — mesma regra de {@link BigDecimalValueObject}, e é o que a tabela
 * de exceptions do projeto define. O aspect de {@code @UseCase} traduz para
 * {@code BadRequestException}.
 *
 * <p><b>Atenção ao implementar:</b>
 *
 * <ul>
 *   <li>{@link #type()} é chamado de dentro do construtor, antes de os campos da subclasse
 *       existirem — devolva uma constante, nunca o valor de um campo.
 *   <li>{@link #customValidate} também é chamado de dentro do construtor, e recebe o valor já
 *       normalizado (trim aplicado) — não dependa de outro estado da instância ali.
 * </ul>
 */
public abstract class StringValueObject extends ValueObject<String> {
    protected StringValueObject(String value) {
        super(value);
    }

    /**
     * Nome do tipo nas mensagens de erro: pt-BR, minúsculo (ex. "nome do evento", "e-mail").
     *
     * <p>Abstrato de propósito. A versão anterior caía em {@code getClass().getSimpleName()}
     * quando a subclasse não sobrescrevia nada — o que produzia mensagem em inglês e capitalizada
     * (ex. "EventName não pode ser nulo") direto do nome da classe Java, sem ninguém perceber
     * porque nenhum teste checava o **início** da mensagem, só o fim.
     */
    protected abstract String type();

    @Override
    protected String validate(String value) {
        if (value == null) {
            throw new ValidationException(type() + " não pode ser nulo");
        }

        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(type() + " não pode ser vazio");
        }

        // trimmed, não value: a subclasse precisa validar o mesmo valor que vai ser guardado,
        // senão checkLength(3, 120) aprova "  ab  " e o VO fica com "ab".
        customValidate(trimmed);

        return trimmed;
    }

    /** Regra própria da subclasse. Recebe o valor já normalizado. */
    protected void customValidate(String value) {
        // a subclasse sobrescreve para acrescentar a própria regra
    }

    protected void checkLength(String value, int min, int max) {
        if (value.length() < min || value.length() > max) {
            throw new ValidationException(type() + " deve ter entre " + min + " e " + max + " caracteres");
        }
    }
}
