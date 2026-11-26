---
name: crm-add-bounded-context
description: Use ao criar um bounded context novo no crm do zero — poms agregador e folhas, registro no core, dependência no boot, migrations e o que o ArchUnit e o Flyway já descobrem sozinhos. Invoque antes de criar qualquer pasta em backend/core/, mesmo que o BC pareça pequeno.
---

# Novo bounded context

As regras (isolamento, camadas, kernel) estão em [`docs/rules/architecture.md`](../../../docs/rules/architecture.md) §3 e §4. Esta skill é só o passo a passo mecânico.

**Antes de começar:** o BC tem feature em `docs/system-design/<feature>/` com `domain-model.md` fechado? Se não, pare e sinalize a lacuna: BC se cria a partir de domínio definido.

## O que você NÃO precisa fazer

- Listar o BC em teste de arquitetura: o `BoundedContextRulesTest` descobre todo `com.crm.<nome>` com camada `domain` (§4.1).
- Registrar Flyway: o `FlywayConfig` descobre toda pasta `db/migration/<módulo>/` com `.sql` (§8).

## Passo a passo

1. **Agregador `backend/core/<bc>/pom.xml`**: copie o de `core/iam/pom.xml` (pai `core`, `packaging=pom`, três `<module>`). Descrição do BC em pt-BR.
2. **Registre** `<module><bc></module>` em `backend/core/pom.xml`.
3. **Três módulos folha**, cada um com `pom.xml` e o `groupId`/`version` do pai. Dependências, e **só estas**:

   | Módulo | Depende de |
   |---|---|
   | `<bc>-domain` | `common-domain`, `shared-domain`, Lombok (`provided`), JUnit (`test`) |
   | `<bc>-application` | `common-application`, `<bc>-domain`, `jakarta.validation-api` (só anotações de request), Lombok, JUnit |
   | `<bc>-infrastructure` | `common-infrastructure`, `<bc>-application`, Lombok, `spring-boot-starter-test` (`test`), mais a biblioteca técnica que o BC precisar |

   **Nunca** `<outro-bc>-*` em dependência nenhuma, e `domain` nunca depende de `application`/`infrastructure`. Versão de terceiro que o Spring Boot não gerencia vai no `dependencyManagement` do `backend/pom.xml`, não no módulo.
4. **Pacotes:** `com.crm.<bc>.domain`, `.application`, `.infrastructure`. É o nome do pacote que o ArchUnit usa para descobrir o BC.
5. **`backend/edge/boot/pom.xml`:** adicione `<bc>-infrastructure`.
6. **`edge/web` e `edge/orchestration`:** adicione `<bc>-application` **só se** o BC expõe endpoint (`web`) ou é composto por um fluxo cross-BC (`orchestration`). `web` nunca depende de `<bc>-domain` nem de `<bc>-infrastructure` (exceção de webhook: [`architecture.md`](../../../docs/rules/architecture.md) §4.3).
7. **Migrations** (se o BC tem tabela): `<bc>-infrastructure/src/main/resources/db/migration/<bc>/V1__<descricao>.sql`. Numeração própria, a partir de `V1`; sem FK para tabela de outro BC; id é `uuid` sem default do banco; `updated_at` nunca `NOT NULL` (§8).
8. **Beans:** uma `@Configuration` por BC na `<bc>-infrastructure` (ex. `<Bc>Beans`), instanciando use cases e Policies com `new`. Sem `@Component`/`@Service` (§9).
9. **Verifique** a partir de `backend/`, em JDK 21: `mvn test`. As regras de arquitetura rodam junto. Detalhe de ambiente: skill `crm-build-and-test`.

## Armadilhas

- Pasta de migration **sem arquivo `.sql`** não conta: o Flyway do módulo não é criado.
- Um BC sem nenhuma classe em `..domain..` é invisível ao ArchUnit: as regras passam vazias, o que é esperado até haver código.
- Se o build quebra com "ciclo" ou "BC depende de outro", há uma dependência entre BCs: a correção é um integration event ou uma orquestração, nunca afrouxar a regra (§5).
