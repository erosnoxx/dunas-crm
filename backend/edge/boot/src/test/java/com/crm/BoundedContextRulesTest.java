package com.crm;

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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garante as fronteiras entre bounded contexts descritas em docs/architecture.md.
 *
 * <p>Este teste mora em {@code boot} porque é o único módulo que enxerga o classpath inteiro:
 * nenhum outro módulo consegue observar que, por exemplo, {@code ordering} não toca
 * {@code party}.
 *
 * <p>Quando uma regra daqui falhar, conserte o código. Adicionar regra nova é legítimo;
 * afrouxar uma existente, não.
 */
@DisplayName("regras de bounded context")
class BoundedContextRulesTest {

    private static final String BASE = "com.crm";

    /** Pacotes de kernel: têm camada domain, mas não são bounded context. */
    private static final Set<String> KERNEL = Set.of("common", "shared");

    /** Descobertos do classpath em {@link #importClasses()}: um BC novo entra sem editar este teste. */
    private static String[] BOUNDED_CONTEXTS;

    /**
     * Proibido na camada de domínio. O domínio valida pelos Value Objects, em código, e não
     * carrega anotação nenhuma, nem Bean Validation.
     */
    private static final String[] FRAMEWORK_FORBIDDEN_IN_DOMAIN = {
        "org.springframework..",
        "jakarta..",
        "javax..",
        "org.hibernate..",
        "com.fasterxml.jackson..",
        "io.github.resilience4j..",
        "com.rabbitmq..",
        "redis.clients.."
    };

    /**
     * Proibido na camada de application. Lista mais estreita que a do domínio de propósito:
     * {@code jakarta.validation} é permitido, porque os schemas de request são validados com Bean
     * Validation. Essas anotações são declarativas, no mesmo espírito do Lombok, e não trazem
     * comportamento de framework para a camada. Tudo que traz comportamento (persistência,
     * transação, servlet) continua fora.
     */
    private static final String[] FRAMEWORK_FORBIDDEN_IN_APPLICATION = {
        "org.springframework..",
        "jakarta.persistence..",
        "jakarta.transaction..",
        "jakarta.servlet..",
        "javax..",
        "org.hibernate..",
        "io.github.resilience4j..",
        "com.rabbitmq..",
        "redis.clients.."
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

        BOUNDED_CONTEXTS = discoverBoundedContexts(classes);
    }

    /**
     * Todo pacote {@code com.crm.<nome>} que tenha uma camada {@code domain} é um bounded context,
     * exceto o kernel (common, shared). Os módulos de borda (boot, web, orchestration) não têm
     * domain, então ficam de fora sozinhos.
     */
    private static String[] discoverBoundedContexts(JavaClasses imported) {
        var prefix = BASE + ".";

        return imported.stream()
            .map(JavaClass::getPackageName)
            .filter(name -> name.startsWith(prefix))
            .map(name -> name.substring(prefix.length()).split("\\.", 3))
            .filter(parts -> parts.length >= 2 && parts[1].equals("domain"))
            .map(parts -> parts[0])
            .filter(name -> !KERNEL.contains(name))
            .collect(Collectors.toCollection(TreeSet::new))
            .toArray(String[]::new);
    }

    @Test
    @DisplayName("nenhum bounded context depende de outro, em camada nenhuma")
    void noBoundedContextDependsOnAnother() {
        for (var context : BOUNDED_CONTEXTS) {
            for (var other : BOUNDED_CONTEXTS) {
                if (context.equals(other))
                    continue;

                check(noClasses()
                    .that().resideInAPackage(bc(context))
                    .should().dependOnClassesThat().resideInAPackage(bc(other))
                    .because("referência entre BCs é UUID cru mais integration event, nunca import direto (architecture.md 4.1)"));
            }
        }
    }

    @Test
    @DisplayName("domain nunca depende de application nem de infrastructure")
    void domainDoesNotDependOnOuterLayers() {
        for (var context : BOUNDED_CONTEXTS) {
            check(noClasses()
                .that().resideInAPackage(layer(context, "domain"))
                .should().dependOnClassesThat().resideInAnyPackage(layer(context, "application"), layer(context, "infrastructure"))
                .because("a direção de dependência é única: domain <- application <- infrastructure"));
        }
    }

    @Test
    @DisplayName("application nunca depende de infrastructure")
    void applicationDoesNotDependOnInfrastructure() {
        for (var context : BOUNDED_CONTEXTS) {
            check(noClasses()
                .that().resideInAPackage(layer(context, "application"))
                .should().dependOnClassesThat().resideInAPackage(layer(context, "infrastructure"))
                .because("a direção de dependência é única: domain <- application <- infrastructure"));
        }
    }

    @Test
    @DisplayName("domain nunca importa framework")
    void domainDoesNotDependOnFramework() {
        for (var context : BOUNDED_CONTEXTS) {
            check(noClasses()
                .that().resideInAPackage(layer(context, "domain"))
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_FORBIDDEN_IN_DOMAIN)
                .because("domain é Java puro: sem Spring, sem JPA, sem framework nenhum"));
        }

        check(noClasses()
            .that().resideInAnyPackage(BASE + ".common.domain..", BASE + ".shared.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_FORBIDDEN_IN_DOMAIN)
            .because("common-domain não depende de nada, e shared-domain carrega conceito de negócio, não framework"));
    }

    @Test
    @DisplayName("application não importa framework além de Bean Validation")
    void applicationDoesNotDependOnFramework() {
        for (var context : BOUNDED_CONTEXTS) {
            check(noClasses()
                .that().resideInAPackage(layer(context, "application"))
                .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_FORBIDDEN_IN_APPLICATION)
                .because("sem Spring e sem persistência em application; a transação vem de @UseCase (architecture.md 7.2)"));
        }

        check(noClasses()
            .that().resideInAPackage(BASE + ".common.application..")
            .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_FORBIDDEN_IN_APPLICATION)
            .because("common-application só tem contratos"));
    }

    @Test
    @DisplayName("common nunca importa bounded context nem o shared kernel")
    void commonDoesNotDependOnBoundedContextsOrSharedKernel() {
        for (var context : BOUNDED_CONTEXTS) {
            check(noClasses()
                .that().resideInAPackage(BASE + ".common..")
                .should().dependOnClassesThat().resideInAPackage(bc(context))
                .because("common é o kernel técnico: não carrega significado de negócio nem conhece BC"));
        }

        check(noClasses()
            .that().resideInAPackage(BASE + ".common..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".shared..")
            .because("common é técnico, shared-kernel é negócio; a dependência só vai de shared para common"));
    }

    @Test
    @DisplayName("shared kernel nunca importa bounded context")
    void sharedKernelDoesNotDependOnBoundedContexts() {
        for (var context : BOUNDED_CONTEXTS) {
            check(noClasses()
                .that().resideInAPackage(BASE + ".shared..")
                .should().dependOnClassesThat().resideInAPackage(bc(context))
                .because("shared-kernel guarda conceitos que vários BCs compartilham, então não pode conhecer nenhum deles"));
        }
    }

    /**
     * A única parte da infrastructure que {@code web} pode ver são as integrações com terceiro,
     * para o controller de webhook entregar o DTO externo ao Translator (architecture.md §4.3).
     * Repository, JPA e o resto continuam proibidos: um controller que os usasse pularia o use
     * case. Quando o {@code pom.xml} de {@code web} depende de uma {@code <bc>-infrastructure}, o
     * Maven deixa de barrar isso; esta regra passa a ser a única cerca.
     */
    @Test
    @DisplayName("web depende da application de um bounded context, e da infrastructure só nas integrações")
    void webDependsOnlyOnApplicationAndIntegrations() {
        for (var context : BOUNDED_CONTEXTS) {
            var infrastructureOutsideIntegrations = resideInAPackage(layer(context, "infrastructure"))
                .and(not(resideInAPackage(BASE + "." + context + ".infrastructure.integrations..")));

            check(noClasses()
                .that().resideInAPackage(BASE + ".web..")
                .should().dependOnClassesThat(infrastructureOutsideIntegrations)
                .orShould().dependOnClassesThat().resideInAPackage(layer(context, "domain"))
                .because("um controller injeta use cases e nada mais; no webhook, também o Translator da integração (architecture.md 4.1 e 4.3)"));
        }
    }

    /**
     * Substituto do antigo Gateway síncrono (architecture.md §5.4): uma classe de web é
     * estruturalmente proibida de compor a application de mais de um bounded context. Todo fluxo
     * que precise de dois BCs no mesmo request TEM que passar pelo módulo {@code orchestration};
     * não há outro jeito de compilar.
     *
     * <p>É isto que torna a convenção real, em vez de melhor esforço: fora este teste, nada impede
     * uma classe futura de importar a application de dois BCs direto dentro de {@code web}.
     */
    @Test
    @DisplayName("nenhuma classe de web compõe a application de mais de um bounded context")
    void webDependsOnAtMostOneBoundedContextPerClass() {
        check(classes()
            .that().resideInAPackage(BASE + ".web..")
            .should(dependOnAtMostOneBoundedContextApplication())
            .because("composição entre BCs mora no módulo orchestration, nunca montada de improviso dentro de web (architecture.md §5.4)"));
    }

    @Test
    @DisplayName("não existe ciclo de dependência entre bounded contexts")
    void noCyclesBetweenBoundedContexts() {
        check(slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles()
            .because("um ciclo entre módulos significa que uma fronteira já foi cruzada nos dois sentidos"));
    }

    /**
     * Uma regra pode não encontrar classe nenhuma, de propósito: um BC sem código não pode
     * quebrar o build. As regras existem antes do código para que a primeira violação real quebre
     * o build na hora.
     */
    private static void check(ArchRule rule) {
        rule.allowEmptyShould(true).check(classes);
    }

    private static String bc(String context) {
        return BASE + "." + context + "..";
    }

    private static String layer(String context, String layer) {
        return BASE + "." + context + "." + layer + "..";
    }

    /**
     * Passa para uma classe que toca, entre as dependências diretas, o pacote de application de
     * no máximo um bounded context. Uma classe de {@code web} que importe tipos de
     * {@code iam.application} e de {@code ticketing.application} ao mesmo tempo viola a regra:
     * essa composição tem que morar em {@code orchestration}.
     */
    private static ArchCondition<JavaClass> dependOnAtMostOneBoundedContextApplication() {
        return new ArchCondition<>("depender da application de no máximo um bounded context") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                var touchedContexts = javaClass.getDirectDependenciesFromSelf().stream()
                    .map(dependency -> dependency.getTargetClass().getPackageName())
                    .map(BoundedContextRulesTest::boundedContextOf)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());

                if (touchedContexts.size() <= 1)
                    return;

                events.add(SimpleConditionEvent.violated(
                    javaClass,
                    javaClass.getName() + " depende da application de mais de um bounded context: " + touchedContexts
                ));
            }
        };
    }

    /**
     * {@link #layer} devolve a sintaxe de wildcard do ArchUnit (termina em {@code ..}), que só
     * vale para {@code resideInAPackage}. Aqui a comparação é {@link String#startsWith} puro,
     * então precisa do nome de pacote real, sem o wildcard, e com o limite de ponto explícito,
     * para "iam.application" não bater em "iam.applicationX" por acidente.
     */
    private static Optional<String> boundedContextOf(String packageName) {
        for (var context : BOUNDED_CONTEXTS) {
            var applicationPackage = BASE + "." + context + ".application";

            if (packageName.equals(applicationPackage) || packageName.startsWith(applicationPackage + "."))
                return Optional.of(context);
        }

        return Optional.empty();
    }
}
