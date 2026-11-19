package com.crm.common.domain.specifications;


import com.crm.common.domain.exceptions.DomainException;

/**
 * Condição de negócio nomeada, que responde sim ou não sobre um candidato.
 *
 * <p>Toda implementação de um BC é uma classe com nome de domínio ({@code SalesOpenSpecification}),
 * nunca uma lambda solta: o nome é o que documenta a regra. Dado externo (hora, limite) entra pelo
 * construtor da implementação; uma Specification nunca consulta repository, port ou relógio.
 *
 * <p>É um predicado em memória, não um filtro de consulta: listagem e busca usam {@code Criteria}.
 * Onde decidir entre Specification, Policy e Domain Service: skill {@code crm-domain-rules}.
 */
@FunctionalInterface
public interface Specification<T> {

    boolean isSatisfiedBy(T candidate);

    /** Satisfeita só quando as duas são. A da direita não é avaliada se a da esquerda já falhou. */
    default Specification<T> and(Specification<T> other) {
        requireOther(other);

        return candidate -> isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    /** Satisfeita quando qualquer uma é. A da direita não é avaliada se a da esquerda já passou. */
    default Specification<T> or(Specification<T> other) {
        requireOther(other);

        return candidate -> isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    default Specification<T> not() {
        return candidate -> !isSatisfiedBy(candidate);
    }

    /**
     * Satisfeita por qualquer candidato. É o elemento neutro do {@code and}: serve de ponto de
     * partida quando a composição é montada aos poucos (ex. só acrescenta uma regra se ela se
     * aplica ao caso).
     */
    static <T> Specification<T> always() {
        return candidate -> true;
    }

    private static void requireOther(Specification<?> other) {
        if (other == null)
            throw new DomainException("specification a compor não pode ser nula");
    }
}
