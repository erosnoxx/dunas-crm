package com.crm.common.application.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Liga um componente de um record {@link com.crm.common.application.repository.Criteria} a um campo
 * da entidade persistida.
 *
 * <p>Componente {@code null} (ou coleção vazia) é ignorado: o filtro só existe quando o chamador o
 * preencheu. {@link #value()} aceita caminho aninhado com ponto ({@code "address.city"}).
 */
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface CriteriaField {
    String value();
    Operator operator() default Operator.EQUALS;

    enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        LESS_THAN_OR_EQUAL_TO,
        LIKE,
        IN
    }
}
