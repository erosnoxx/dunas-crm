package com.crm.shared.domain.vos.identity.validators;

import com.crm.common.domain.exceptions.ValidationException;

/**
 * Valida CNPJ pelos dois dígitos verificadores (módulo 11).
 *
 * <p>Aceita o formato alfanumérico, em que cada posição pode ser dígito ou letra maiúscula; sequências
 * de um caractere só são recusadas.
 */
public final class CnpjValidator {

    private static final int[] FIRST_DIGIT_WEIGHTS = {
            5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2
    };

    private static final int[] SECOND_DIGIT_WEIGHTS = {
            6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2
    };

    private CnpjValidator() {
    }

    public static boolean isValid(String value) {
        if (!value.matches("[A-Z0-9]{14}"))
            return false;

        if (hasAllSameCharacters(value))
            return false;

        int firstDigit = calculateDigit(
                value,
                FIRST_DIGIT_WEIGHTS
        );

        if (firstDigit != value.charAt(12) - '0')
            return false;

        int secondDigit = calculateDigit(
                value,
                SECOND_DIGIT_WEIGHTS
        );

        return secondDigit == value.charAt(13) - '0';
    }

    private static boolean hasAllSameCharacters(String value) {
        char first = value.charAt(0);

        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) != first)
                return false;
        }

        return true;
    }

    private static int calculateDigit(
            String value,
            int[] weights
    ) {
        int sum = 0;

        for (int i = 0; i < weights.length; i++) {
            sum += toNumericValue(value.charAt(i)) * weights[i];
        }

        int remainder = sum % 11;

        return remainder < 2
                ? 0
                : 11 - remainder;
    }

    private static int toNumericValue(char character) {
        if (character >= '0' && character <= '9')
            return character - '0';

        if (character >= 'A' && character <= 'Z')
            return character - 48;

        throw new ValidationException(
                "caractere de CNPJ inválido: " + character
        );
    }
}