package com.crm.common.infrastructure.persistence.repositories;

import com.crm.common.application.annotations.CriteriaField;
import com.crm.common.application.exceptions.InternalServerErrorException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Converte um record {@code Criteria} em predicados JPA, lendo as anotações
 * {@link CriteriaField} de cada componente.
 */
public class CriteriaUtils {
    public static <E, C> Predicate[] buildPredicates(
            C criteria,
            Root<E> root,
            CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // getRecordComponents() devolve null para qualquer coisa que não seja record, o que
        // estouraria NPE aqui embaixo. Falhar com mensagem clara é melhor do que o filtro
        // ficar mudo (query sem WHERE) ou quebrar com stack trace incompreensível.
        var components = criteria.getClass().getRecordComponents();

        if (components == null)
            throw new InternalServerErrorException(
                    "criteria precisa ser um record: " + criteria.getClass().getName()
            );

        for (RecordComponent component : components) {
            var annotation = component.getAnnotation(CriteriaField.class);
            if (annotation == null) continue;

            try {
                Object value = component.getAccessor().invoke(criteria);
                if (value == null) continue;
                if (value instanceof Collection<?> c && c.isEmpty()) continue;

                Path<Object> path = resolvePath(root, annotation.value());
                predicates.add(buildPredicate(cb, path, value, annotation.operator()));

            } catch (Exception e) {
                throw new InternalServerErrorException(
                        "falha ao montar o filtro do campo " + component.getName() + " do criteria", e
                );
            }
        }

        return predicates.toArray(new Predicate[0]);
    }

    private static <E> Path<Object> resolvePath(Root<E> root, String field) {
        String[] parts = field.split("\\.");
        Path<Object> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate buildPredicate(
            CriteriaBuilder cb,
            Path<Object> path,
            Object value,
            CriteriaField.Operator operator) {
        return switch (operator) {
            case EQUALS -> cb.equal(path, value);
            case NOT_EQUALS -> cb.notEqual(path, value);
            case GREATER_THAN -> cb.greaterThan(path.as(Comparable.class), (Comparable) value);
            case LESS_THAN -> cb.lessThan(path.as(Comparable.class), (Comparable) value);
            case GREATER_THAN_OR_EQUAL_TO -> cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            case LESS_THAN_OR_EQUAL_TO -> cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
            case LIKE -> cb.like(
                    cb.lower(path.as(String.class)),
                    "%" + value.toString().toLowerCase() + "%"
            );
            case IN -> {
                if (!(value instanceof Collection<?> collection)) {
                    throw new InternalServerErrorException("operador IN exige um valor do tipo Collection");
                }
                yield path.in(collection);
            }
        };
    }
}
