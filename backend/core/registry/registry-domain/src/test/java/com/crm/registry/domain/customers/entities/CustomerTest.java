package com.crm.registry.domain.customers.entities;

import com.crm.common.domain.exceptions.DomainException;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.contact.Phone;
import com.crm.shared.domain.vos.identity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Customer")
class CustomerTest {

    private static final Document CPF = Document.of("529.982.247-25");
    private static final Document CNPJ = Document.of("12.ABC.345/01DE-35");
    private static final CustomerName NAME = CustomerName.of("Maria Souza");
    private static final CustomerName OTHER_NAME = CustomerName.of("Maria Souza Lima");
    private static final CustomerName TRADE = CustomerName.of("Solar Sul");
    private static final Email EMAIL = Email.of("maria@x.com");
    private static final Phone PHONE = Phone.of("+5511999999999");

    private static void assertFailsWith(Runnable action, String code, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    assertThat(((DomainException) e).getCode()).contains(code);
                    assertThat(e).hasMessage(message);
                });
    }

    @Test
    @DisplayName("nasce ativo só com telefone")
    void registersActiveWithOnlyAPhone() {
        var customer = Customer.register(CPF, NAME, null, null, PHONE);

        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getVersion()).isEmpty();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.isActive()).isTrue();
        assertThat(customer.getEmail()).isEmpty();
        assertThat(customer.getPhone()).contains(PHONE);
        assertThat(customer.getTradeName()).isEmpty();
    }

    @Test
    @DisplayName("nasce ativo só com e-mail")
    void registersActiveWithOnlyAnEmail() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, null);

        assertThat(customer.getEmail()).contains(EMAIL);
        assertThat(customer.getPhone()).isEmpty();
    }

    @Test
    @DisplayName("cadastro sem contato falha")
    void registerWithoutContactFails() {
        assertFailsWith(() -> Customer.register(CPF, NAME, null, null, null),
                "contact_required", "informe pelo menos um contato: e-mail ou telefone");
    }

    @Test
    @DisplayName("pessoa física com nome fantasia falha")
    void registerCpfWithTradeNameFails() {
        assertFailsWith(() -> Customer.register(CPF, NAME, TRADE, EMAIL, null),
                "trade_name_not_allowed", "nome fantasia só vale para pessoa jurídica");
    }

    @Test
    @DisplayName("pessoa jurídica aceita nome fantasia")
    void registerCnpjWithTradeName() {
        var customer = Customer.register(CNPJ, NAME, TRADE, EMAIL, null);

        assertThat(customer.getTradeName()).contains(TRADE);
    }

    @Test
    @DisplayName("edita nome, fantasia e contatos, nunca o documento")
    void editChangesNameTradeNameAndContactButNeverTheDocument() {
        var customer = Customer.register(CNPJ, NAME, null, EMAIL, null);

        customer.edit(OTHER_NAME, TRADE, null, PHONE);

        assertThat(customer.getDocument()).isEqualTo(CNPJ);
        assertThat(customer.getName()).isEqualTo(OTHER_NAME);
        assertThat(customer.getTradeName()).contains(TRADE);
        assertThat(customer.getEmail()).isEmpty();
        assertThat(customer.getPhone()).contains(PHONE);
    }

    @Test
    @DisplayName("edição pode largar um contato, não os dois")
    void editCanDropOneContactButNotBoth() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, PHONE);

        customer.edit(NAME, null, EMAIL, null);

        assertThat(customer.getPhone()).isEmpty();
        assertFailsWith(() -> customer.edit(NAME, null, null, null),
                "contact_required", "informe pelo menos um contato: e-mail ou telefone");
    }

    @Test
    @DisplayName("edição pode largar o nome fantasia")
    void editCanDropTheTradeName() {
        var customer = Customer.register(CNPJ, NAME, TRADE, EMAIL, null);

        customer.edit(NAME, null, EMAIL, null);

        assertThat(customer.getTradeName()).isEmpty();
    }

    @Test
    @DisplayName("edição de pessoa física com nome fantasia falha")
    void editCpfWithTradeNameFails() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, null);

        assertFailsWith(() -> customer.edit(NAME, TRADE, EMAIL, null),
                "trade_name_not_allowed", "nome fantasia só vale para pessoa jurídica");
    }

    @Test
    @DisplayName("cliente desativado sem contato falha como desativado")
    void editOfADeactivatedCustomerWithoutContactFailsAsDeactivated() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, null);
        customer.deactivate();

        assertFailsWith(() -> customer.edit(NAME, TRADE, null, null),
                "customer_deactivated", "este cliente está desativado");
    }

    @Test
    @DisplayName("desativa e reativa")
    void deactivatesAndReactivates() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, null);

        customer.deactivate();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
        assertThat(customer.isActive()).isFalse();

        customer.reactivate();
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    @DisplayName("desativar duas vezes falha")
    void deactivateTwiceFails() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, null);
        customer.deactivate();

        assertFailsWith(customer::deactivate, "customer_deactivated", "este cliente está desativado");
    }

    @Test
    @DisplayName("reativar um ativo falha")
    void reactivateAnActiveOneFails() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, null);

        assertFailsWith(customer::reactivate, "customer_not_deactivated", "este cliente não está desativado");
    }

    @Test
    @DisplayName("reconstitui mantendo id e versão, sem validar de novo")
    void reconstituteKeepsIdAndVersion() {
        var id = UUID.randomUUID();

        var customer = Customer.reconstitute(id, 3L, CPF, NAME, null, EMAIL, null, CustomerStatus.DEACTIVATED);

        assertThat(customer.getId()).isEqualTo(id);
        assertThat(customer.getVersion()).contains(3L);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("não registra domain event")
    void registersNoDomainEvent() {
        var customer = Customer.register(CPF, NAME, null, EMAIL, null);
        customer.deactivate();
        customer.reactivate();
        customer.edit(NAME, null, EMAIL, null);

        assertThat(customer.pullEvents()).isEmpty();
    }

    @Test
    @DisplayName("mensagens de erro nunca carregam documento, e-mail nem telefone")
    void errorMessagesNeverCarryTheDocument() {
        var active = Customer.register(CNPJ, NAME, null, EMAIL, PHONE);
        var deactivated = Customer.register(CPF, NAME, null, EMAIL, PHONE);
        deactivated.deactivate();

        Runnable[] failures = {
                () -> Customer.register(CPF, NAME, null, null, null),
                () -> Customer.register(CPF, NAME, TRADE, EMAIL, PHONE),
                () -> active.edit(NAME, null, null, null),
                () -> deactivated.edit(NAME, null, EMAIL, null),
                deactivated::deactivate,
                active::reactivate
        };

        for (var failure : failures)
            assertThatThrownBy(failure::run)
                    .isInstanceOf(DomainException.class)
                    .satisfies(e -> assertThat(e.getMessage())
                            .doesNotContain(CPF.getValue(), CNPJ.getValue(), EMAIL.getValue(), PHONE.getValue()));
    }
}
