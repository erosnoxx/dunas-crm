package com.crm;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Anti-Corruption Layer: nenhum modelo de sistema de terceiro (Mercado Pago, Google, provedor de
 * e-mail) chega ao domínio. Toda integração mora em {@code <bc>.infrastructure.integrations.<nome>},
 * e o {@code <Nome>Translator} dela é a única classe que conhece o modelo externo e o nosso ao
 * mesmo tempo. Ver docs/architecture.md §4.3.
 *
 * <p>Não vale para a nossa própria infraestrutura (Postgres, Redis, RabbitMQ): o modelo dela é
 * nosso.
 *
 * <p>As regras existem antes da primeira integração, de propósito: a primeira violação quebra o
 * build na hora. Quando uma falhar, conserte o código. Adicionar regra nova é legítimo; afrouxar
 * uma existente, não.
 */
@DisplayName("regras de anti-corruption layer")
class AntiCorruptionLayerRulesTest {

    private static final String BASE = "com.crm";

    private static final String INTEGRATIONS = "..infrastructure.integrations..";
    private static final String EXTERNAL_DTOS = "..infrastructure.integrations.*.dtos..";

    /** Raiz de uma integração: o pacote {@code ...infrastructure.integrations.<nome>}. */
    private static final Pattern INTEGRATION_ROOT = Pattern.compile("^(.*\\.infrastructure\\.integrations\\.[^.]+)");

    /**
     * Pacotes pelos quais o sistema fala com terceiros: clientes HTTP e envio de e-mail. **Ao
     * adicionar uma integração que use SDK próprio (ex. {@code com.mercadopago..}), registre o
     * pacote do SDK aqui**: com SDK, o modelo externo são as classes dele, e fora desta lista
     * nada impede que elas vazem.
     */
    private static final String[] THIRD_PARTY_ACCESS = {
        "org.springframework.web.client..",
        "org.springframework.web.reactive.function.client..",
        "java.net.http..",
        "org.springframework.mail..",
        "jakarta.mail.."
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
    }

    @Test
    @DisplayName("só uma integração fala com sistema de terceiro")
    void onlyIntegrationsTalkToThirdParties() {
        check(noClasses()
            .that().resideOutsideOfPackage(INTEGRATIONS)
            .should().dependOnClassesThat().resideInAnyPackage(THIRD_PARTY_ACCESS)
            .because("toda chamada a terceiro passa por <bc>.infrastructure.integrations.<nome>, com Translator (architecture.md §4.3)"));
    }

    @Test
    @DisplayName("DTO externo só é acessado pela própria integração")
    void externalDtosStayInsideTheirIntegration() {
        check(classes()
            .that().resideInAPackage(EXTERNAL_DTOS)
            .should(beAccessedOnlyFromTheirOwnIntegration())
            .because("o modelo do terceiro não pode ser lido fora da integração que o traduz (architecture.md §4.3)"));
    }

    @Test
    @DisplayName("toda implementação de contrato numa integração usa o Translator dela")
    void integrationImplementationsUseTheirTranslator() {
        check(classes()
            .that().resideInAPackage(INTEGRATIONS)
            .and().resideOutsideOfPackage(EXTERNAL_DTOS)
            .and().haveSimpleNameEndingWith("ServiceImpl")
            .should(dependOnTheTranslatorOfTheirIntegration())
            .because("repassar dado do terceiro 1:1, sem traduzir, é o que a ACL existe para impedir (architecture.md §4.3)"));
    }

    @Test
    @DisplayName("Translator só existe dentro de uma integração")
    void translatorsLiveOnlyInIntegrations() {
        check(classes()
            .that().haveSimpleNameEndingWith("Translator")
            .should().resideInAPackage(INTEGRATIONS)
            .andShould().resideOutsideOfPackage(EXTERNAL_DTOS)
            .because("traduzir modelo externo é papel exclusivo da integração, na infraestrutura (architecture.md §4.3)"));
    }

    /** Uma regra pode não encontrar classe nenhuma: enquanto não há integração, passam vazias. */
    private static void check(ArchRule rule) {
        rule.allowEmptyShould(true).check(classes);
    }

    private static ArchCondition<JavaClass> beAccessedOnlyFromTheirOwnIntegration() {
        return new ArchCondition<>("ser acessado só por classes da própria integração") {
            @Override
            public void check(JavaClass dto, ConditionEvents events) {
                var integration = integrationOf(dto.getPackageName());

                dto.getAccessesToSelf().stream()
                    .filter(access -> !integrationOf(access.getOriginOwner().getPackageName()).equals(integration))
                    .forEach(access -> events.add(SimpleConditionEvent.violated(
                        dto,
                        access.getOriginOwner().getName() + " acessa o DTO externo " + dto.getName() + " de fora da integração"
                    )));
            }
        };
    }

    private static ArchCondition<JavaClass> dependOnTheTranslatorOfTheirIntegration() {
        return new ArchCondition<>("depender do Translator da própria integração") {
            @Override
            public void check(JavaClass implementation, ConditionEvents events) {
                var integration = integrationOf(implementation.getPackageName());

                var usesOwnTranslator = implementation.getDirectDependenciesFromSelf().stream()
                    .map(Dependency::getTargetClass)
                    .anyMatch(target -> target.getSimpleName().endsWith("Translator")
                        && integrationOf(target.getPackageName()).equals(integration));

                if (usesOwnTranslator)
                    return;

                events.add(SimpleConditionEvent.violated(
                    implementation,
                    implementation.getName() + " não usa o Translator da própria integração"
                ));
            }
        };
    }

    private static Optional<String> integrationOf(String packageName) {
        var matcher = INTEGRATION_ROOT.matcher(packageName);

        if (!matcher.find())
            return Optional.empty();

        return Optional.of(matcher.group(1));
    }
}
