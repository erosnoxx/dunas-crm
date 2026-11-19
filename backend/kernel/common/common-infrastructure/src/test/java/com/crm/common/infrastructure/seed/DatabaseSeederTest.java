package com.crm.common.infrastructure.seed;

import com.crm.common.application.exceptions.InternalServerErrorException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DatabaseSeeder")
class DatabaseSeederTest {

    record Item(String email) {
    }

    /** Seeder falso: guarda o que recebeu e se a transação estava aberta na hora. */
    static final class FakeSeeder implements Seeder<Item> {
        private final String resourcePath;
        private final boolean alreadySeeded;
        private final boolean failing;
        private final TransactionState transaction;

        final List<Item> received = new ArrayList<>();
        boolean seededInsideTransaction;

        FakeSeeder(String resourcePath, boolean alreadySeeded, boolean failing, TransactionState transaction) {
            this.resourcePath = resourcePath;
            this.alreadySeeded = alreadySeeded;
            this.failing = failing;
            this.transaction = transaction;
        }

        @Override
        public boolean isAlreadySeeded() {
            return alreadySeeded;
        }

        @Override
        public void seed(List<Item> data) {
            seededInsideTransaction = transaction.open;
            received.addAll(data);

            if (failing)
                throw new InternalServerErrorException("falha ao semear");
        }

        @Override
        public String resourcePath() {
            return resourcePath;
        }

        @Override
        public Class<Item> recordType() {
            return Item.class;
        }
    }

    static final class TransactionState {
        boolean open;
    }

    private final TransactionState transaction = new TransactionState();
    private MockEnvironment environment;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        transactionTemplate = mock(TransactionTemplate.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            transaction.open = true;

            try {
                return invocation.getArgument(0, TransactionCallback.class).doInTransaction(null);
            }
            finally {
                transaction.open = false;
            }
        });
    }

    private DatabaseSeeder seederOf(Seeder<?>... seeders) {
        return new DatabaseSeeder(List.of(seeders), new ObjectMapper(), environment, transactionTemplate);
    }

    @Test
    @DisplayName("pula o seeder que já foi semeado")
    void skipsSeederThatIsAlreadySeeded() throws Exception {
        var seeder = new FakeSeeder("seed/plain.json", true, false, transaction);

        seederOf(seeder).run(null);

        assertThat(seeder.received).isEmpty();
    }

    @Test
    @DisplayName("semeia dentro de uma transação, com os placeholders resolvidos")
    void seedsInsideATransaction() throws Exception {
        environment.setProperty("CRM_ADMIN_EMAIL", "admin@example.com");
        var seeder = new FakeSeeder("seed/admin.json", false, false, transaction);

        seederOf(seeder).run(null);

        assertThat(seeder.seededInsideTransaction).isTrue();
        assertThat(seeder.received).containsExactly(new Item("admin@example.com"));
    }

    @Test
    @DisplayName("falha o boot quando falta a variável de um placeholder, sem expor outros valores")
    void failsBootWhenAPlaceholderIsMissing() {
        environment.setProperty("CRM_OUTRA", "valor-secreto");
        var seeder = new FakeSeeder("seed/admin.json", false, false, transaction);

        assertThatThrownBy(() -> seederOf(seeder).run(null))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("seed: variável não definida: CRM_ADMIN_EMAIL");

        assertThat(seeder.received).isEmpty();
    }

    @Test
    @DisplayName("a falha de um seeder sobe e impede os seguintes")
    void failurePropagatesAndStopsNextSeeders() {
        var first = new FakeSeeder("seed/plain.json", false, true, transaction);
        var second = new FakeSeeder("seed/plain.json", false, false, transaction);

        assertThatThrownBy(() -> seederOf(first, second).run(null))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("falha ao semear");

        assertThat(second.received).isEmpty();
    }
}
