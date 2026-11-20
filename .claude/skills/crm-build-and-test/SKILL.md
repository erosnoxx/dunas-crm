---
name: crm-build-and-test
description: Use ao compilar, rodar testes ou investigar falha de build no crm — JDK 21 obrigatório, comandos por módulo, teste que precisa de Docker, testes de arquitetura. Invoque quando mvn falhar com erro que não aponta a causa (Lombok não gera código, Mockito não mocka classe, "client version 1.32 is too old") e antes de dar uma tarefa de backend por concluída.
---

# Build e testes do backend

Tudo roda a partir de `backend/`. Critério de aceite de qualquer mudança no backend: **`mvn test` verde** ([`git-workflow.md`](../../../docs/rules/git-workflow.md) §4).

## Ambiente

- **JDK 21, sempre.** O `maven-enforcer-plugin` recusa outro, com mensagem pedindo o `JAVA_HOME`. Num JDK mais novo, o Lombok não gera nada e o Mockito não mocka classe concreta, com erros que não apontam a causa.
- Maven 3.9 ou superior.
- Aponte o `JAVA_HOME` para um JDK 21 na sessão (o IntelliJ costuma baixá-lo em `~/.jdks/`). Não altere o `pom.xml` para contornar.

## Comandos

```
mvn test                                              tudo
mvn -pl kernel/common/common-infrastructure -am test  um módulo e o que ele precisa
mvn test -Dtest=BoundedContextRulesTest               só um teste (vale no build inteiro)
```

O `failIfNoSpecifiedTests=false` está no pai: `-Dtest=` não falha nos módulos que não contêm o teste.

## Testes de arquitetura

Moram em `edge/boot/src/test/java/com/crm/` (único módulo que vê o classpath inteiro): `BoundedContextRulesTest`, `ClockRulesTest`, `AntiCorruptionLayerRulesTest`. O que cada um trava: [`architecture.md`](../../../docs/rules/architecture.md) §11.

**Teste de arquitetura falhou = conserte o código.** Afrouxar a regra não é legítimo. Adicionar regra é.

## Teste que precisa de Docker

Três classes usam Testcontainers e falham sem Docker rodando:

| Classe | Módulo | Sobe |
|---|---|---|
| `IdempotencyGuardPostgresTest` | `common-infrastructure` | Postgres |
| `IamPersistencePostgresTest` | `iam-infrastructure` | Postgres |
| `IamEndToEndTest` | `edge/boot` | Postgres e Redis, com a aplicação inteira num Tomcat de verdade |

Para o resto do build:

```
mvn test -Dtest='!*PostgresTest,!IamEndToEndTest'
```

O `IamEndToEndTest` é o único teste do contexto real da aplicação: é ele que acha erro de ligação (bean que falta, listener que não grava, cache que não é invalidado). Mudança na cadeia de segurança, num listener ou numa `@Configuration` só termina com ele verde.

**Falha intermitente conhecida:** de vez em quando o Testcontainers estoura o tempo de 10 s esperando o container subir. Rode de novo uma vez antes de suspeitar do código.

Diga no PR quando esse teste não pôde rodar; não o omita. O Testcontainers está fixado em `1.21.4` no pai: o do BOM do Spring Boot (1.20.6) fala com o Docker pela API 1.32, que o Docker Engine 29 recusa (`client version 1.32 is too old`). Subir o Spring Boot é rever essa versão.

## Sintoma → causa

| Sintoma | Causa provável |
|---|---|
| enforcer: "compila com JDK 21" | `JAVA_HOME` num JDK diferente |
| `cannot find symbol` em getter/builder de Lombok | JDK errado, ou processador faltando em `annotationProcessorPaths` do pai |
| Mockito "Cannot mock/inline" | JDK errado |
| `client version 1.32 is too old` | Testcontainers desatualizado frente ao Docker Engine |
| teste de arquitetura aponta dependência entre BCs | dependência de verdade: ver `architecture.md` §5 |
| `.properties` com texto estranho | acento em arquivo `.properties` (lido como ISO-8859-1): escreva sem acento |
| Flyway não cria o histórico de um módulo | pasta `db/migration/<módulo>/` sem arquivo `.sql` |

## Ao escrever teste

Hora fixa (`Instant.parse(...)` ou `Clock.fixed(...)`), `@DisplayName` em pt-BR: [`code-style.md`](../../../docs/rules/code-style.md) §8. Teste de Translator usa payload real do terceiro, sem rede (skill `crm-integration-acl`).
