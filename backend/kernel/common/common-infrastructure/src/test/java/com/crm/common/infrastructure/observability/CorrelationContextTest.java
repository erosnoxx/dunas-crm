package com.crm.common.infrastructure.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationContext")
class CorrelationContextTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("open põe os dois ids no MDC e close os remove")
    void putsAndRemoves() {
        try (var ignored = CorrelationContext.open("req-1", "corr-1")) {
            assertThat(MDC.get(CorrelationContext.REQUEST_ID)).isEqualTo("req-1");
            assertThat(CorrelationContext.correlationId()).contains("corr-1");
        }

        assertThat(MDC.get(CorrelationContext.REQUEST_ID)).isNull();
        assertThat(CorrelationContext.correlationId()).isEmpty();
    }

    @Test
    @DisplayName("um scope aninhado restaura os ids do externo ao fechar")
    void nestedScopeRestoresTheOuterOne() {
        try (var outer = CorrelationContext.open("req-outer", "corr-outer")) {
            try (var inner = CorrelationContext.open("req-inner", "corr-inner")) {
                assertThat(CorrelationContext.correlationId()).contains("corr-inner");
            }

            assertThat(MDC.get(CorrelationContext.REQUEST_ID)).isEqualTo("req-outer");
            assertThat(CorrelationContext.correlationId()).contains("corr-outer");
        }
    }

    @Test
    @DisplayName("openNew usa o mesmo id nos dois campos")
    void openNewIsTheRootOfItsOwnTrail() {
        try (var ignored = CorrelationContext.openNew()) {
            assertThat(MDC.get(CorrelationContext.REQUEST_ID))
                    .isNotBlank()
                    .isEqualTo(MDC.get(CorrelationContext.CORRELATION_ID));
        }
    }

    @Test
    @DisplayName("isValid recusa nulo, vazio, espaço, quebra de linha e mais de 64 caracteres")
    void validation() {
        assertThat(CorrelationContext.isValid("a1-B2_c3.d4")).isTrue();
        assertThat(CorrelationContext.isValid("a".repeat(64))).isTrue();

        assertThat(CorrelationContext.isValid(null)).isFalse();
        assertThat(CorrelationContext.isValid("")).isFalse();
        assertThat(CorrelationContext.isValid("a b")).isFalse();
        assertThat(CorrelationContext.isValid("a\nb")).isFalse();
        assertThat(CorrelationContext.isValid("a".repeat(65))).isFalse();
    }

    @Test
    @DisplayName("MdcTaskDecorator leva o MDC para a thread do executor e o limpa depois")
    void taskDecoratorCarriesTheMdc() throws Exception {
        var seen = new AtomicReference<String>();
        Runnable decorated;

        try (var ignored = CorrelationContext.open("req-1", "corr-async")) {
            decorated = new MdcTaskDecorator().decorate(() -> seen.set(MDC.get(CorrelationContext.CORRELATION_ID)));
        }

        // numa thread nova e sem MDC, como a de um pool
        var thread = new Thread(decorated);
        thread.start();
        thread.join();

        assertThat(seen.get()).isEqualTo("corr-async");
    }
}
