# Estilo de código — crm

Regras de formatação e estrutura que valem para **todo** código Java do projeto, em qualquer módulo. São convenções fixas, não sugestões.

O que o ArchUnit já trava (framework por camada, leitura de hora, fronteira de BC) está em [`architecture.md`](architecture.md) e **não se repete aqui**. Este documento cobre o que ferramenta nenhuma barra.

---

## 1. Nunca usar `else`

Fail fast. Guard clause no topo, retorno antecipado, `orElseThrow`. Vale para `if/else` e para ternário usado como `if/else` denso.

```java
// errado
public void changeQuantity(Quantity quantity) {
    if (quantity.isZero()) {
        throw new DomainException("a quantidade precisa ser maior que zero");
    } else {
        this.quantity = quantity;
    }
}

// correto
public void changeQuantity(Quantity quantity) {
    if (quantity.isZero())
        throw new DomainException("a quantidade precisa ser maior que zero");

    this.quantity = quantity;
}
```

## 2. Chaves e `catch`

- `if` (e `for`) de **uma instrução** não leva chaves: a instrução vem na linha seguinte, indentada. Com mais de uma instrução, leva chaves.
- `catch` abre em **linha própria**, depois do `}`:

```java
try {
    return joinPoint.proceed();
}
catch (ApplicationException e) {
    throw e;
}
```

## 3. Argumentos longos e linhas verticais

Construtor ou chamada com mais de 2 ou 3 parâmetros: **um argumento por linha**, mesmo que caiba numa linha só.

```java
public static Visit create(
    VisitDate date,
    Technician technician,
    UUID customerId,
    Instant now
) {
    ...
}
```

Expressão longa vira passos intermediários com `var`, em vez de uma linha com várias chamadas encadeadas:

```java
// evitar
return repository.findById(id).map(mapper::toResponse).orElseThrow(() -> new NotFoundException("visita não encontrada: " + id));

// preferir
var visit = repository.findById(id)
    .orElseThrow(() -> new NotFoundException("visita não encontrada: " + id));

return mapper.toResponse(visit);
```

## 4. Blocos lógicos separados por linha em branco

Cada etapa de um método (buscar, validar, mutar, persistir, publicar) é um parágrafo isolado por uma linha em branco, mesmo com uma instrução só:

```java
public void execute(UUID visitId) {
    var visit = repository.findById(visitId)
        .orElseThrow(() -> new NotFoundException("visita não encontrada: " + visitId));

    visit.cancel(Instant.now(clock));

    repository.save(visit);

    publisher.publishAll(visit.pullEvents());
}
```

## 5. Texto: idioma e mensagens

O que é nome de código é inglês; o que é texto para gente ler é português.

| Texto | Idioma |
|---|---|
| Identificadores (classe, método, campo, variável, pacote) | inglês |
| Comentário e Javadoc | português (pt-BR) |
| Mensagem de exceção e de log | português (pt-BR), minúsculo, sem ponto final |
| `@DisplayName` de teste e `.because(...)` de ArchUnit | português (pt-BR) |
| Código de erro (`code`) | inglês, `snake_case` |

**Mensagem de exceção:** começa com letra minúscula, sem ponto final, direta, sem jargão técnico. Ela pode chegar ao usuário pelo frontend, e traduzir lá não é viável.

```java
throw new NotFoundException("visita não encontrada: " + id);
throw new ConflictException("já existe uma visita neste horário: " + slot);
throw new DomainException("não é possível cancelar uma visita já concluída");
```

A mensagem de exceção **nunca carrega valor sensível** (senha, token, documento completo): o aspect de `@UseCase` a escreve no log ([`architecture.md`](architecture.md) §7.2).

**Comentário:** diz o **porquê** que o código não mostra, nunca o que ele faz. **Toda classe de produção tem Javadoc** em pt-BR, dizendo para que ela existe e a decisão que não é óbvia. Não escreva estado de implementação ("ainda vazio", "falta fazer"): fica errado no commit seguinte.

## 6. Exceptions

Todo código de `domain`, `application` e `infrastructure` lança uma exception **do sistema**. Nunca `IllegalArgumentException`, `IllegalStateException`, `RuntimeException`, `NullPointerException`, nem exception de framework ou driver lançada diretamente.

Há duas hierarquias independentes: `DomainException` (domínio) e `ApplicationException` (application e infrastructure, com status HTTP). A tradução entre elas é do aspect de `@UseCase` e está em [`architecture.md`](architecture.md) §7.3. O que decide qual lançar:

| Situação | Exception |
|---|---|
| valor inválido num Value Object | `ValidationException` (domínio) |
| invariante de negócio violada | `DomainException` (domínio) |
| entrada malformada, com lista de campos | `BadRequestException` |
| sem autenticação válida / credencial inválida | `UnauthorizedException` |
| autenticado, sem permissão | `ForbiddenException` |
| recurso não existe | `NotFoundException` |
| conflito de estado (duplicidade, já usado, concorrência) | `ConflictException` |
| regra de negócio violada, decidida pelo use case | `UnprocessableEntityException` |
| limite de taxa excedido | `RateLimitExceededException` |
| falha de mecanismo interno (crypto, parsing, serialização) | `InternalServerErrorException` |
| falha de sistema externo (gateway, e-mail, API de fabricante) | `ExternalServiceException` |

Todas exigem mensagem. Consequências:

- **O domínio lança `DomainException`/`ValidationException`** e não importa tipo de application (`<bc>-domain` nem enxerga essa camada).
- **Não escreva `try/catch` em use case** só para converter exception de domínio: o aspect já converte.
- O use case lança direto o tipo de application quando a decisão é **dele** (não achou o aggregate, conflito de estado).
- Para o frontend distinguir erros de mesmo status, use o construtor `(code, message)`. O código é contrato público.

**Proxy obrigatório na infraestrutura.** Exception nativa ou de terceiro capturada em classe de infraestrutura (persistência, crypto, e-mail, API externa, mensageria) é traduzida antes de subir: falha de comunicação com terceiro vira `ExternalServiceException`; falha de mecanismo interno vira `InternalServerErrorException`; violação de constraint vira `ConflictException`, o que `RepositoryImpl` já faz para todo repository que o estende. Nunca deixe a original vazar crua.

## 7. Lombok e anotações

Lombok é permitido em qualquer camada, inclusive `domain` (`@Getter`, `@RequiredArgsConstructor`, `@Slf4j`). Use `@RequiredArgsConstructor` para injeção por construtor final. Quais anotações de **framework** cada camada aceita está em [`architecture.md`](architecture.md) §4.1 e é travado pelo ArchUnit.

## 8. Testes

- `@DisplayName` em pt-BR em toda classe e método de teste.
- **Hora fixa**: `Instant.parse(...)` ou `Clock.fixed(...)`, nunca a hora real. É o que mantém o resultado sempre igual.
- Teste de VO cobre o valor nulo (a base recusa com `ValidationException`), o inválido e o válido.

---

## Checklist rápido

- [ ] Nenhum `else`
- [ ] `if` de uma instrução sem chaves; `catch` em linha própria
- [ ] 3+ parâmetros em formato vertical; sem cadeia longa numa linha só
- [ ] Blocos lógicos separados por linha em branco
- [ ] Identificadores em inglês; comentário, log, mensagem e `@DisplayName` em pt-BR
- [ ] Mensagem de exceção minúscula, sem ponto final, sem valor sensível
- [ ] Toda classe de produção com Javadoc
- [ ] Nenhuma exception nativa lançada; nenhuma de terceiro vazando da infraestrutura
- [ ] Teste com hora fixa
