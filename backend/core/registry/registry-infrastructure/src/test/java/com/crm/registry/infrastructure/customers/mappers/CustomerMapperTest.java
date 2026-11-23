package com.crm.registry.infrastructure.customers.mappers;

import com.crm.registry.domain.customers.entities.Customer;
import com.crm.registry.domain.customers.enums.CustomerStatus;
import com.crm.registry.domain.customers.vos.CustomerName;
import com.crm.shared.domain.vos.contact.Email;
import com.crm.shared.domain.vos.contact.Phone;
import com.crm.shared.domain.vos.identity.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomerMapper")
class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    @DisplayName("ida e volta preserva todos os campos e a versão")
    void roundTripKeepsEveryField() {
        var customer = Customer.reconstitute(
                UUID.randomUUID(),
                3L,
                Document.of("11.222.333/0001-81"),
                CustomerName.of("Solar Ltda"),
                CustomerName.of("Solar"),
                Email.of("contato@solar.com"),
                Phone.of("+5511999990000"),
                CustomerStatus.DEACTIVATED
        );

        var entity = mapper.toPersistence(customer);
        var back = mapper.toDomain(entity);

        assertThat(entity.getDocument()).isEqualTo("11222333000181");
        assertThat(back.getId()).isEqualTo(customer.getId());
        assertThat(back.getVersion()).contains(3L);
        assertThat(back.getDocument()).isEqualTo(customer.getDocument());
        assertThat(back.getName()).isEqualTo(customer.getName());
        assertThat(back.getTradeName()).isEqualTo(customer.getTradeName());
        assertThat(back.getEmail()).isEqualTo(customer.getEmail());
        assertThat(back.getPhone()).isEqualTo(customer.getPhone());
        assertThat(back.getStatus()).isEqualTo(CustomerStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("versão nula é linha nova")
    void nullVersionIsNewRow() {
        var entity = mapper.toPersistence(Customer.register(
                Document.of("529.982.247-25"), CustomerName.of("Maria"), null, Email.of("maria@x.com"), null));

        assertThat(entity.getVersion()).isNull();
        assertThat(mapper.toDomain(entity).getVersion()).isEmpty();
    }

    @Test
    @DisplayName("opcionais ausentes viram coluna nula e voltam ausentes")
    void absentOptionalsStayAbsent() {
        var entity = mapper.toPersistence(Customer.register(
                Document.of("529.982.247-25"), CustomerName.of("Maria"), null, null, Phone.of("+5511999990000")));

        assertThat(entity.getTradeName()).isNull();
        assertThat(entity.getEmail()).isNull();

        var back = mapper.toDomain(entity);

        assertThat(back.getTradeName()).isEmpty();
        assertThat(back.getEmail()).isEmpty();
        assertThat(back.getPhone()).isPresent();
    }

    @Test
    @DisplayName("createdAt e updatedAt da persistência não chegam ao domínio")
    void timestampsStayInPersistence() {
        var entity = mapper.toPersistence(Customer.register(
                Document.of("529.982.247-25"), CustomerName.of("Maria"), null, Email.of("maria@x.com"), null));

        entity.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));

        var again = mapper.toPersistence(mapper.toDomain(entity));

        assertThat(again.getCreatedAt()).isNull();
        assertThat(again.getUpdatedAt()).isNull();
    }
}
