package com.crm.registry.infrastructure.catalog.persistence;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Diz qual constraint uma falha de gravação violou, para o repository trocar o conflito genérico
 * pelo código de erro do nome repetido. O nome vem do índice único da migration.
 */
final class UniqueConstraint {

    private UniqueConstraint() {
    }

    static boolean isViolated(Throwable error, String constraint) {
        for (var cause = error; cause != null; cause = cause.getCause())
            if (cause instanceof ConstraintViolationException violation && constraint.equalsIgnoreCase(violation.getConstraintName()))
                return true;

        return false;
    }
}
