package com.crm;

import com.crm.common.infrastructure.common.TimeConfig;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * O sistema tem uma fonte de hora só: o bean {@link Clock} declarado em {@link TimeConfig}.
 * Código de produção obtém "agora" desse clock (ou recebe um {@code Instant now} derivado dele),
 * nunca direto do sistema operacional. É isso que deixa toda regra dependente de tempo
 * determinística nos testes, e mantém o sistema inteiro num fuso só.
 *
 * <p>{@code System.nanoTime()} é permitido: mede duração, não hora do relógio.
 *
 * <p>Quando uma regra daqui falhar, conserte o código. Afrouxar uma regra não é legítimo.
 */
@DisplayName("regras de relógio")
class ClockRulesTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.crm");
    }

    @Test
    @DisplayName("nenhum código de produção lê a hora atual sem o Clock")
    void noDirectTimeReads() {
        noClasses()
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now")
            .orShould().callMethod(LocalDate.class, "now", ZoneId.class)
            .orShould().callMethod(LocalDateTime.class, "now")
            .orShould().callMethod(LocalDateTime.class, "now", ZoneId.class)
            .orShould().callMethod(LocalTime.class, "now")
            .orShould().callMethod(LocalTime.class, "now", ZoneId.class)
            .orShould().callMethod(OffsetDateTime.class, "now")
            .orShould().callMethod(OffsetDateTime.class, "now", ZoneId.class)
            .orShould().callMethod(OffsetTime.class, "now")
            .orShould().callMethod(OffsetTime.class, "now", ZoneId.class)
            .orShould().callMethod(ZonedDateTime.class, "now")
            .orShould().callMethod(ZonedDateTime.class, "now", ZoneId.class)
            .orShould().callMethod(Year.class, "now")
            .orShould().callMethod(Year.class, "now", ZoneId.class)
            .orShould().callMethod(YearMonth.class, "now")
            .orShould().callMethod(YearMonth.class, "now", ZoneId.class)
            .orShould().callMethod(MonthDay.class, "now")
            .orShould().callMethod(MonthDay.class, "now", ZoneId.class)
            .orShould().callMethod(System.class, "currentTimeMillis")
            .orShould().callConstructor(Date.class)
            .because("a única fonte de hora é o bean Clock de TimeConfig: use X.now(clock) ou receba um Instant now")
            .check(classes);
    }

    @Test
    @DisplayName("só TimeConfig constrói um relógio de sistema")
    void onlyTimeConfigBuildsTheSystemClock() {
        noClasses()
            .that().doNotHaveFullyQualifiedName(TimeConfig.class.getName())
            .should().callMethod(Clock.class, "systemUTC")
            .orShould().callMethod(Clock.class, "systemDefaultZone")
            .orShould().callMethod(Clock.class, "system", ZoneId.class)
            .because("um segundo relógio de sistema poderia rodar em outro fuso: injete o bean Clock")
            .check(classes);
    }
}
