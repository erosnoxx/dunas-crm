---
name: crm-add-persistence
description: Use ao persistir um aggregate no crm — migration Flyway do BC, entidade JPA, mapper domínio↔JPA, contrato `Repository`, `Criteria` com `@CriteriaField` e a implementação `Jpa<Nome>RepositoryImpl`. Invoque antes de criar qualquer SQL em `db/migration/<bc>/`, qualquer `@Entity` ou qualquer repository.
---

# Persistência de um aggregate

Regras e motivo: [`architecture.md`](../../../docs/rules/architecture.md) §8 (migrations, lock otimista, `created_at`/`updated_at`), §4.1 (FK cross-BC proibida) e §10.1 (nomes). Referência viva, completa: `RepositoryImpl`, `EntityMapper`, `JpaEntity` e o teste `RepositoryImplTest`, em `common-infrastructure`.

Um aggregate = uma tabela raiz (mais as das entidades internas dele). Entidade interna **não** tem repository: o mapper do root a carrega e salva.

## Ordem

1. **Migration.** `<bc>-infrastructure/src/main/resources/db/migration/<bc>/V<n>__<descricao>.sql`, numerada de `V1` **por módulo** (independente dos outros). O Flyway descobre a pasta sozinho; nada a registrar.

   ```sql
   CREATE TABLE visit (
       id          UUID PRIMARY KEY,
       customer_id UUID        NOT NULL,   -- outro BC: uuid cru, SEM REFERENCES
       status      VARCHAR(20) NOT NULL,
       version     BIGINT      NOT NULL,
       created_at  TIMESTAMPTZ NOT NULL,
       updated_at  TIMESTAMPTZ             -- nunca NOT NULL: o primeiro INSERT o deixa nulo
   );
   ```

   - **Id sem default**: o `UUID` vem do domínio. Sem sequence, identity nem `@GeneratedValue`.
   - **Coluna `version`** em toda tabela de aggregate (lock otimista).
   - **FK cross-BC é proibida** e o ArchUnit não lê SQL: vive na revisão.
   - Unicidade de negócio é `UNIQUE` no banco: a violação vira `ConflictException` sozinha.
   - Migration já publicada **não se edita**: nova versão.
2. **Entidade JPA** em `<bc>-infrastructure`, estendendo `JpaEntity<UUID>` (que traz `id`, `createdAt`, `updatedAt` e `@Version`):

   ```java
   @Entity
   @Table(name = "visit")
   @Getter @Setter
   @NoArgsConstructor
   class VisitEntity extends JpaEntity<UUID> {
       private UUID customerId;

       @Enumerated(EnumType.STRING)
       private VisitStatus status;
   }
   ```

   Colunas guardam o **valor cru** (`String`, `BigDecimal`, `LocalDate`), nunca o VO: o domínio não tem anotação JPA. Enum com `EnumType.STRING`. `createdAt`/`updatedAt` são preenchidos pelo listener, com o `Clock`.
3. **Mapper** `EntityMapper<A, P>`:
   - `toDomain` chama o `reconstitute(id, version, ...)` do aggregate, reconstruindo cada VO com `Vo.of(...)`.
   - `toPersistence` copia o `id` e a versão (`aggregate.getVersion().orElse(null)`: nula significa linha nova) e os campos. **Nunca** leva `createdAt`/`updatedAt` ao domínio.
4. **Contrato** em `<bc>-application`, `application.contracts.repositories`: `interface VisitRepository extends Repository<Visit, VisitCriteria>`. Nome limpo. Acrescente método só se o use case precisar (ex. `findByEmail`); transição que duas requisições disputam por natureza é um **update condicional** no contrato, não um `save` com 409.
5. **Criteria**, em `application`: **`record`** que implementa `Criteria`, componentes anotados com `@CriteriaField("<atributo da entity>")` (aceita caminho `a.b` e `Operator`). Componente nulo ou coleção vazia é ignorado.
6. **Spring Data** `interface VisitJpaRepository extends JpaRepository<VisitEntity, UUID>` e a **implementação** `JpaVisitRepositoryImpl extends RepositoryImpl<Visit, VisitCriteria, VisitEntity, VisitJpaRepository> implements VisitRepository` (tecnologia no início, `Impl` no fim, §10.1), passando `em`, o mapper e `VisitEntity.class` ao `super`.
7. **Registro** do mapper e do repository na `<Bc>Beans` (`new`, sem `@Component`, §9). Entidade e `JpaRepository` sob `com.crm` são descobertos pela `CrmApplication`; nada a configurar.

## Comportamento que você herda (não reescreva)

- `save` grava **na hora** (flush) e devolve o aggregate com a versão nova.
- Versão velha vira `ConcurrentUpdateException` (409); violação de constraint vira `ConflictException`. Nenhuma exception de JPA/Spring sobe crua.
- **O repository não publica evento**: é do use case ([`crm-add-use-case`](../crm-add-use-case/SKILL.md)).

## Teste

- **Mapper:** ida e volta (`toPersistence` → `toDomain`) preserva todos os campos e a versão; versão nula ↔ linha nova.
- **Repository:** unitário com Mockito, como o `RepositoryImplTest`. Um teste com Postgres real (Testcontainers) exige Docker; para rodar sem ele, `-Dtest='!IdempotencyGuardPostgresTest'` ([`crm-build-and-test`](../crm-build-and-test/SKILL.md)).
- A migration só é validada contra a entity com o banco de pé (`ddl-auto=validate`): um erro de nome de coluna aparece no boot, não na compilação.

## Checklist

- [ ] Migration em `db/migration/<bc>/`, `V<n>` do módulo, id sem default, `version`, `updated_at` nulável
- [ ] Nenhuma FK para tabela de outro BC
- [ ] Entity estende `JpaEntity<UUID>`; colunas cruas, sem VO
- [ ] Mapper leva a versão, não leva `createdAt`/`updatedAt`
- [ ] Contrato em `application.contracts.repositories`; `Criteria` é `record`
- [ ] `Jpa<Nome>RepositoryImpl` estende `RepositoryImpl`; beans na `<Bc>Beans`
- [ ] Testes de mapper e repository; `mvn test` verde
