package com.crm.registry.infrastructure;

import org.springframework.context.annotation.Configuration;

/**
 * Registra todos os beans do BC {@code registry} (architecture.md §9): sem {@code @Component} nem
 * {@code @Service}, use case, repository e listener são instanciados aqui, com {@code new}.
 *
 * <p>Vazia por enquanto: os beans entram junto com o código de cada subdomínio.
 */
@Configuration
public class RegistryBeans {
}
