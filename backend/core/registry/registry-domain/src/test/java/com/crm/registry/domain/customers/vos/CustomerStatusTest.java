package com.crm.registry.domain.customers.vos;

import com.crm.registry.domain.customers.enums.CustomerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomerStatus")
class CustomerStatusTest {

    @Test
    @DisplayName("tem os valores ativo e desativado")
    void hasActiveAndDeactivated() {
        assertThat(CustomerStatus.values()).containsExactly(CustomerStatus.ACTIVE, CustomerStatus.DEACTIVATED);
    }
}
