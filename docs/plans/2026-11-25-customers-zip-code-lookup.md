# Consulta automática de CEP: plano

Spec: [`ADR-34`](../system-design/03-customers/decisions.md) e [`usecases.md`](../system-design/03-customers/usecases.md). O domínio não muda.

## Backend (`registry`)
1. `ZipCodeAddress` (schema) e `ZipCodeLookupService` (contrato) em `application.customers`.
2. `FindAddressByZipCode` (`@UseCase`, `transactional = false`): papel de editor, `ZipCode.of`, `zip_code_not_found`. Bean em `RegistryBeans`.
3. `registry.infrastructure.integrations.brasilapi`: `BrasilApiClient` (`RestClient`, 404/400 vazio, 5xx e 429 transitórios), `BrasilApiTranslator`, `BrasilApiZipCodeLookupService` (retry), `BrasilApiProperties` (`crm.brasilapi.*`, com `@DefaultValue`), `BrasilApiConfig`, `dtos/`. `spring-web` e `jackson-databind` no pom da infraestrutura.
4. `ZipCodesController`: `GET /zip-codes/{zipCode}`.
5. Testes: use case, Translator com payload real, service com `MockRestServiceServer`, controller.

## Frontend
1. `findZipCode` em `customers.service.ts`.
2. `location-form.tsx`: com 8 dígitos consulta; achado, preenche, trava cidade e UF (`readOnly` e `disabled`) e foca o número; mudar o CEP destrava; falha libera tudo com aviso `role="status"`; resposta de CEP já trocado é ignorada.
3. Testes da tela.

## Critério
`mvn test` e `npm run typecheck && npm run lint && npm test && npm run build && npm run format:check`.
