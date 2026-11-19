package com.crm.shared.domain.vos.identity.validators;

/**
 * Valida CPF pelos dois dígitos verificadores (módulo 11). Espera 11 dígitos, sem máscara;
 * sequências de um dígito só são recusadas.
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    public static boolean isValid(String value) {
        if (!value.matches("\\d{11}"))
            return false;

        if (hasAllSameDigits(value))
            return false;

        int firstDigit = calculateFirstDigit(value);

        if (firstDigit != value.charAt(9) - '0')
            return false;

        int secondDigit = calculateSecondDigit(value);

        return secondDigit == value.charAt(10) - '0';
    }

    private static boolean hasAllSameDigits(String value) {
        char first = value.charAt(0);

        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) != first)
                return false;
        }

        return true;
    }

    private static int calculateFirstDigit(String value) {
        int sum = 0;

        for (int i = 0; i < 9; i++)
            sum += (value.charAt(i) - '0') * (10 - i);

        int remainder = sum % 11;

        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int calculateSecondDigit(String value) {
        int sum = 0;

        for (int i = 0; i < 10; i++)
            sum += (value.charAt(i) - '0') * (11 - i);

        int remainder = sum % 11;

        return remainder < 2 ? 0 : 11 - remainder;
    }
}