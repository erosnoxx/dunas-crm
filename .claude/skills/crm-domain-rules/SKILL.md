---
name: crm-domain-rules
description: Use ao decidir onde mora uma regra de negócio no crm — entidade/VO, Specification, Policy, Domain Service ou máquina de estados — e ao escrever qualquer uma delas. Invoque quando um `if` de negócio aparecer num use case, quando a mesma condição se repetir em dois lugares, ou quando um aggregate ganhar status com transições.
---

# Onde mora a regra de negócio

A tabela e as restrições estão em [`architecture.md`](../../../docs/rules/architecture.md) §6.4. **O use case só orquestra** (§7.1): um `if` de negócio nele desce para uma das peças abaixo. Esta skill é como escolher e como escrever.

## Escolha

```
Precisa ser verdade para o objeto existir válido?        → dentro da entidade ou do VO
É uma condição sim/não, reutilizável ou combinável?      → Specification
A decisão muda conforme o contexto (variantes)?          → Policy
Liga aggregates do mesmo BC e não tem dono natural?      → Domain Service
É o ciclo de vida (status e transições) de um aggregate? → State machine
```

Na dúvida entre entidade e as demais: comece na entidade. Só extraia quando a regra aparecer em **dois** lugares ou precisar de dado que a entidade não tem.

## Specification

`Specification<T>` (`common-domain`): `isSatisfiedBy(T)`, com `and`, `or`, `not`, `always()`.

- **Classe com nome de domínio** (`SalesOpenSpecification`), nunca lambda solta: o nome documenta a regra.
- Dado externo (hora, limite) entra pelo **construtor**. Nunca consulta repository, port nem relógio.
- É predicado em memória. Busca e listagem continuam em `Criteria` (`crm-add-persistence`).

## Policy

Decisão que varia com o contexto (ex. como arredondar um orçamento, qual janela de visita vale para um tipo de cliente).

- **Interface própria por decisão**, com assinatura própria, e **uma implementação por variante**. Não existe interface genérica `Policy` nem `@Policy`.
- Dado externo entra por **parâmetro ou construtor**, nunca por consulta.
- Sem estado. A interface mora em `domain`; a escolha da variante é do use case ou da `<Bc>Beans`, que instancia com `new` (§9).

## Domain Service

Regra que liga aggregates do **mesmo** BC e não pertence a nenhum.

- Sem estado, **sem repository**: recebe os aggregates já carregados.
- **Nunca cruza BC**: isso é orquestração (§5.4), não domínio.
- Sem anotação (`@DomainService` não existe). Instanciado com `new` na `<Bc>Beans`.

## State machine

`StateMachine<V extends Enum<V>>` (`common-domain`) é só a base: guarda o estado atual (`value`) e o evento da última transição (`event`). **Nenhum BC a usa ainda**, então o padrão de estados e transições não está fixado.

- A **primeira feature com ciclo de vida** (proposta, visita) define o padrão, e esta seção passa a descrevê-lo, a partir do código que passou nos testes.
- Antes disso, **não invente**: proponha o desenho ao usuário. Transição inválida é `DomainException` com código estável, e cada transição registra o domain event.

## Teste

JUnit puro. Specification e Policy: um caso por ramo, com a hora/limite fixos pelo construtor. Domain Service: aggregates montados à mão, sem mock de repository (ele não tem).

## Checklist

- [ ] Nenhum `if` de negócio no use case
- [ ] Specification com nome de domínio e dado externo no construtor
- [ ] Policy com interface própria; nenhuma interface genérica
- [ ] Domain Service sem estado, sem repository, sem cruzar BC
- [ ] Máquina de estados só depois de combinar o padrão com o usuário
