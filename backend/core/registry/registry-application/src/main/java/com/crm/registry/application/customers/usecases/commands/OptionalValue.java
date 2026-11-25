package com.crm.registry.application.customers.usecases.commands;

import java.util.function.Function;

/** Converte um campo opcional: nulo é ausente; qualquer texto, inclusive em branco, passa pelo VO. */
public final class OptionalValue {

    private OptionalValue() {
    }

    public static <T> T of(String value, Function<String, T> factory) {
        return value == null ? null : factory.apply(value);
    }
}
