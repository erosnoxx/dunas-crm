package com.crm.iam.domain.vos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enums do IAM")
class EnumsTest {

    @Test
    @DisplayName("Role, UserStatus e TokenPurpose têm exatamente os valores da spec")
    void hasSpecValues() {
        assertThat(Role.values()).extracting(Enum::name)
                .containsExactly("ADMINISTRATOR", "COMMERCIAL", "TECHNICIAN");
        assertThat(UserStatus.values()).extracting(Enum::name)
                .containsExactly("INVITED", "ACTIVE", "DEACTIVATED");
        assertThat(TokenPurpose.values()).extracting(Enum::name)
                .containsExactly("INVITATION", "PASSWORD_RESET");
    }
}
