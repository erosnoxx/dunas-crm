package com.crm.common.domain.specifications;

import com.crm.common.domain.exceptions.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Specification")
class SpecificationTest {

    private static final Specification<Integer> POSITIVE = number -> number > 0;
    private static final Specification<Integer> EVEN = number -> number % 2 == 0;

    @Nested
    @DisplayName("and")
    class And {

        @Test
        @DisplayName("é satisfeita só quando as duas são")
        void satisfiedOnlyWhenBothAre() {
            var positiveAndEven = POSITIVE.and(EVEN);

            assertTrue(positiveAndEven.isSatisfiedBy(4));
            assertFalse(positiveAndEven.isSatisfiedBy(3));
            assertFalse(positiveAndEven.isSatisfiedBy(-4));
            assertFalse(positiveAndEven.isSatisfiedBy(-3));
        }

        @Test
        @DisplayName("não avalia a da direita quando a da esquerda já falhou")
        void shortCircuits() {
            var evaluated = new AtomicBoolean(false);
            Specification<Integer> tracked = number -> {
                evaluated.set(true);
                return true;
            };

            POSITIVE.and(tracked).isSatisfiedBy(-1);

            assertFalse(evaluated.get());
        }

        @Test
        @DisplayName("recusa compor com nula")
        void rejectsNull() {
            var exception = assertThrows(DomainException.class, () -> POSITIVE.and(null));

            assertEquals("specification a compor não pode ser nula", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("or")
    class Or {

        @Test
        @DisplayName("é satisfeita quando qualquer uma é")
        void satisfiedWhenEitherIs() {
            var positiveOrEven = POSITIVE.or(EVEN);

            assertTrue(positiveOrEven.isSatisfiedBy(4));
            assertTrue(positiveOrEven.isSatisfiedBy(3));
            assertTrue(positiveOrEven.isSatisfiedBy(-4));
            assertFalse(positiveOrEven.isSatisfiedBy(-3));
        }

        @Test
        @DisplayName("não avalia a da direita quando a da esquerda já foi satisfeita")
        void shortCircuits() {
            var evaluated = new AtomicBoolean(false);
            Specification<Integer> tracked = number -> {
                evaluated.set(true);
                return false;
            };

            POSITIVE.or(tracked).isSatisfiedBy(1);

            assertFalse(evaluated.get());
        }

        @Test
        @DisplayName("recusa compor com nula")
        void rejectsNull() {
            var exception = assertThrows(DomainException.class, () -> POSITIVE.or(null));

            assertEquals("specification a compor não pode ser nula", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("not")
    class Not {

        @Test
        @DisplayName("inverte o resultado")
        void inverts() {
            var notPositive = POSITIVE.not();

            assertFalse(notPositive.isSatisfiedBy(1));
            assertTrue(notPositive.isSatisfiedBy(-1));
        }
    }

    @Test
    @DisplayName("composição encadeada segue a ordem das chamadas")
    void chainedComposition() {
        // (positivo e par) ou (não positivo)
        var composed = POSITIVE.and(EVEN).or(POSITIVE.not());

        assertTrue(composed.isSatisfiedBy(2));
        assertTrue(composed.isSatisfiedBy(-3));
        assertFalse(composed.isSatisfiedBy(3));
    }

    @Nested
    @DisplayName("always")
    class Always {

        @Test
        @DisplayName("é satisfeita por qualquer candidato")
        void satisfiedByAnything() {
            assertTrue(Specification.<Integer>always().isSatisfiedBy(-1));
            assertTrue(Specification.<Integer>always().isSatisfiedBy(null));
        }

        @Test
        @DisplayName("é neutra no and: compor com ela não muda o resultado")
        void isNeutralForAnd() {
            var composed = Specification.<Integer>always().and(POSITIVE);

            assertTrue(composed.isSatisfiedBy(1));
            assertFalse(composed.isSatisfiedBy(-1));
        }
    }
}
