package com.crm.registry.application.catalog;

import com.crm.common.application.exceptions.ApplicationException;
import com.crm.registry.application.catalog.schemas.CharacteristicInput;
import com.crm.shared.application.Actor;
import com.crm.shared.domain.enums.Role;
import org.assertj.core.api.ThrowingConsumer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Dados de teste compartilhados pelos testes dos use cases do catálogo. */
public final class CatalogFixtures {

    public static final Actor ADMINISTRATOR = actor(Role.ADMINISTRATOR);
    public static final Actor COMMERCIAL = actor(Role.COMMERCIAL);
    public static final Actor TECHNICIAN = actor(Role.TECHNICIAN);

    public static final CharacteristicInput POWER = new CharacteristicInput("Potência", "KILOWATT", true);

    private CatalogFixtures() {
    }

    public static Actor actor(Role role) {
        return new Actor(UUID.randomUUID(), role, UUID.randomUUID());
    }

    public static ThrowingConsumer<Throwable> applicationCode(String code) {
        return e -> assertThat(((ApplicationException) e).getCode()).contains(code);
    }
}
