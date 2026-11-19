# backend

Módulos Maven, a partir de `backend/`:

```
kernel/common   técnico (domain, application, infrastructure)
kernel/shared   negócio compartilhado (VOs, schemas)
core/<bc>       um BC: <bc>-domain, <bc>-application, <bc>-infrastructure
edge/           orchestration, web, boot (único deployável)
```

Estrutura, regra de dependência e o que mora em cada módulo: [`docs/rules/architecture.md`](../docs/rules/architecture.md) §3 e §4. Este arquivo não os repete.

## Rodar e testar

`mvn test` a partir desta pasta, em **JDK 21**. Um teste (`IdempotencyGuardPostgresTest`) exige Docker. Ambiente, comandos e falhas comuns: skill `crm-build-and-test`.

## Antes de mexer

- BC novo: skill `crm-add-bounded-context`.
- Chamada a sistema de fora: skill `crm-integration-acl`.
- Estilo: [`docs/rules/code-style.md`](../docs/rules/code-style.md).

## Armadilhas do escopo

- Arquivo `.properties` é lido como ISO-8859-1: **sem acento** em comentário e valor.
- `edge/boot` é o único módulo que vê o classpath inteiro, e por isso hospeda os testes de arquitetura e a configuração que cruza BCs. Config de um BC só mora na `<bc>-infrastructure` dele.
- `shared-infrastructure` vazio é legítimo: não force conteúdo.
