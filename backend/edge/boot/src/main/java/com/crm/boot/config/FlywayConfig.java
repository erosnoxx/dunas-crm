package com.crm.boot.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Uma instância de Flyway por módulo dono de schema, cada uma com sua própria location e sua
 * própria tabela de histórico.
 *
 * <p>Os módulos são descobertos do classpath: toda pasta {@code db/migration/<módulo>/} que
 * contenha migrations vira um Flyway com location {@code classpath:db/migration/<módulo>} e
 * tabela {@code flyway_schema_history_<módulo>}. Um bounded context novo só precisa colocar suas
 * migrations nessa pasta; não há bean nem lista para editar aqui.
 *
 * <p>Mora em {@code boot} porque é a única configuração que enxerga todos os módulos ao mesmo
 * tempo — ver {@code docs/architecture.md} §8 para o porquê de não haver numeração global.
 */
@Configuration
public class FlywayConfig {

    private static final String MIGRATIONS_ROOT = "db/migration/";

    /** Registra os beans {@code <módulo>Flyway}; roda antes de qualquer bean ser instanciado. */
    @Bean
    static BeanDefinitionRegistryPostProcessor flywayModules() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                for (var module : discoverModules()) {
                    BeanDefinition definition = BeanDefinitionBuilder
                            .genericBeanDefinition(Flyway.class, () -> moduleFlyway(
                                    ((BeanFactory) registry).getBean(DataSource.class),
                                    module
                            ))
                            .setInitMethodName("migrate")
                            .getBeanDefinition();

                    registry.registerBeanDefinition(beanName(module), definition);
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            }
        };
    }

    /**
     * Garante que as migrations rodem <b>antes</b> de o Hibernate inicializar.
     *
     * <p>Com a instância automática do Spring Boot desligada, essa ordem deixa de ser garantida:
     * o {@code EntityManagerFactory} poderia subir contra um schema ainda não migrado e falhar a
     * validação, ou pior, passar por acidente dependendo da ordem de criação dos beans.
     */
    @Bean
    static EntityManagerFactoryDependsOnPostProcessor flywayMigrationsBeforeEntityManager() {
        var beanNames = discoverModules().stream().map(FlywayConfig::beanName).toArray(String[]::new);

        return new EntityManagerFactoryDependsOnPostProcessor(beanNames) {
        };
    }

    /**
     * Nomes das pastas em {@code db/migration/} que tenham ao menos um {@code .sql}, em ordem
     * alfabética. Lê arquivos, e não diretórios, porque é o que existe tanto em disco quanto
     * dentro de um jar.
     */
    static Set<String> discoverModules() {
        try {
            var modules = new TreeSet<String>();

            for (Resource migration : new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:" + MIGRATIONS_ROOT + "*/*.sql")) {
                var url = migration.getURL().toString();
                var afterRoot = url.substring(url.lastIndexOf(MIGRATIONS_ROOT) + MIGRATIONS_ROOT.length());

                modules.add(afterRoot.substring(0, afterRoot.indexOf('/')));
            }

            return modules;
        } catch (IOException e) {
            throw new UncheckedIOException("não foi possível listar as migrations do classpath", e);
        }
    }

    static String beanName(String module) {
        return module + "Flyway";
    }

    /**
     * {@code baselineOnMigrate} é necessário, não conveniência: cada módulo tem histórico
     * próprio, então o segundo Flyway a rodar encontra um schema que já tem tabelas mas não tem
     * a <b>sua</b> tabela de histórico. Sem baseline, ele recusaria migrar um schema não vazio.
     *
     * <p>O baseline fica na versão 0, antes da V1 de qualquer módulo. Com o padrão do Flyway
     * (versão 1), a V1 do segundo módulo a rodar seria dada como aplicada sem nunca rodar.
     */
    private static Flyway moduleFlyway(DataSource dataSource, String module) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:" + MIGRATIONS_ROOT + module)
                .table("flyway_schema_history_" + module)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }
}
