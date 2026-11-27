package com.crm.web.iam;

import com.crm.web.iam.resolvers.ActorArgumentResolver;
import com.crm.shared.application.Actor;
import com.crm.shared.domain.enums.Role;
import com.crm.web.middlewares.RestExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

/** Monta o MockMvc standalone dos controllers de IAM, com o mesmo tratamento de erro e o resolvedor de {@link Actor}. */
public final class IamMvc {

    public static final Actor ADMINISTRATOR = new Actor(
            UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
            Role.ADMINISTRATOR,
            UUID.fromString("00000000-0000-0000-0000-0000000000b1")
    );

    private IamMvc() {
    }

    public static MockMvc standalone(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .setCustomArgumentResolvers(new ActorArgumentResolver())
                .build();
    }
}
