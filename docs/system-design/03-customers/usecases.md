# Use cases: 03-customers

Forma dos use cases (sem interface, sem sufixo, `@UseCase`, `commands` e `queries`): [`architecture.md`](../../rules/architecture.md) §7. Domínio: [`domain-model.md`](domain-model.md). Telas: [`design/`](design/README.md).

> Aprovado em 2026-11-22. Os mockups de [`design/`](design/README.md) foram aprovados em 2026-11-22, e as respostas das queries fecharam com eles (ADR-27 a ADR-29); os pontos que eram *proposta* foram aceitos com a aprovação (ADR-33).

## Convenções desta feature

**Quem age.** Todo use case recebe o `Actor` de `shared-application` ([`architecture.md`](../../rules/architecture.md) §3.1), já montado pela `web`, e checa o papel **no use case** com `actor.requireAnyRole(...)`, que lança `ForbiddenException` com o código `forbidden`. A regra "quem escreve no cadastro de clientes" mora numa classe do subdomínio, `CustomerAccess` (`requireEditor`, `requireReader`), no molde do `CatalogAccess`, para os 12 commands não a repetirem. É classe própria, e não o `CatalogAccess`, porque quem edita clientes pode deixar de ser quem edita o catálogo.

**Papéis** (ADR-7):

| | Administrador | Comercial | Técnico |
|---|---|---|---|
| Consultar cliente, local e equipamento | ✔ | ✔ | ✔ |
| Cadastrar, editar, desativar e reativar | ✔ | ✔ | recusado (`forbidden`, 403) |

**Ordem das pré-condições**, em todos os commands: **papel** (403) → **formato** dos campos, ao montar os VOs (400) → **existe** (404) → **ativo** (422) → **regra do aggregate** sobre campos já válidos (`contact_required`, `trade_name_not_allowed`: 422) → **unicidade** (409). A ordem é contrato: um Técnico com dado inválido recebe 403, e um documento repetido de cliente com formato inválido recebe 400. As regras de "ativo" e do aggregate rodam na chamada do método de domínio (`register`, `install`, `edit`), que vem antes da checagem de unicidade.

**Pacotes.** `com.crm.registry.application.customers.usecases.commands` e `.queries`; contratos em `com.crm.registry.application.customers.contracts.{repositories,readers}`. Do catálogo, os commands de equipamento usam o `EquipmentModelRepository` (ADR-8).

**Listagens.** `page` e `size` como no catálogo (padrão 20, máximo 100, cortados pela `web`). Ordem padrão por nome. `status` padrão `ACTIVE`; `ALL` mostra os dois.

**Relógio.** Só os commands de equipamento recebem o `Clock` (por construtor), para a regra "data de instalação não é depois de hoje" (`InstallationDate.of(valor, clock)`). Os demais não leem hora.

**Campo opcional ausente.** Nome fantasia, e-mail, telefone, complemento e data de instalação ausentes são `null` (ou o campo omitido no JSON); texto em branco não é ausência, é 400 do VO ("... não pode ser vazio"). A tela envia `null` para o campo opcional vazio (ADR-31).

## Commands: cliente

### `RegisterCustomer`

- **Entrada:** ator, `document`, `name`, `tradeName` (opcional, só CNPJ: ADR-21), `email` (opcional), `phone` (opcional, E.164; a tela envia `+55` + DDD + número: ADR-18); **ao menos um contato** (ADR-16).
- **Pré-condições:** 1. papel Administrador ou Comercial (`forbidden`). 2. Formato: `Document`, `CustomerName` (nome e nome fantasia), `Email`, `Phone` (400). 3. Ao menos um contato (`contact_required`, 422) e nome fantasia só com CNPJ (`trade_name_not_allowed`, 422). 4. Documento ainda não usado, entre ativos e desativados (`ConflictException`, `customer_document_taken`).
- **Fluxo:** monta os VOs, `Customer.register`, confere o documento no repository, salva.
- **Retorno:** `UUIDResponse`. **Endpoint:** `POST /customers` (201).

### `UpdateCustomer`

- **Entrada:** ator, `customerId`, `name`, `tradeName`, `email`, `phone`. O documento **não** vem: não muda, e não há command de correção (ADR-3, ADR-12, ADR-24). Nome fantasia, e-mail ou telefone ausente apaga o valor, mas não os dois contatos.
- **Pré-condições:** 1. papel; 2. formato (400); 3. cliente existe (`NotFoundException`, `customer_not_found`); 4. está ativo (`customer_deactivated`); 5. ao menos um contato (`contact_required`, 422) e nome fantasia só com CNPJ (`trade_name_not_allowed`, 422).
- **Fluxo:** carrega, `edit`, salva (lock otimista: `concurrent_update`, 409).
- **Endpoint:** `PUT /customers/{customerId}`.

## Commands: local

### `RegisterLocation`

- **Entrada:** ator, `customerId`, `name`, `address` (`zipCode`, `street`, `number`, `complement` opcional, `neighborhood`, `city`, `state`).
- **Pré-condições:** papel; formato de `LocationName` e de cada campo do `Address` (400; o CEP só no formato, 8 dígitos: ADR-19; `state` fora das 27 siglas também é 400); cliente existe (`customer_not_found`) e está ativo (`customer_deactivated`).
- **Fluxo:** carrega o cliente, `Location.register(customer, name, address)`, salva.
- **Retorno:** `UUIDResponse`. **Endpoint:** `POST /customers/{customerId}/locations` (201).

### `UpdateLocation`

- **Entrada:** ator, `locationId`, `name`, `address`. O cliente não vem: não muda.
- **Pré-condições:** papel; formato (400); local existe (`location_not_found`) e está ativo (`location_deactivated`). O cliente **não** é conferido: desativado, ele só bloqueia local novo (ADR-6).
- **Fluxo:** carrega, `edit`, salva.
- **Endpoint:** `PUT /locations/{locationId}`.

## Commands: equipamento instalado

### `InstallEquipment`

- **Entrada:** ator, `locationId`, `modelId`, `serialNumber`, `installedOn` (opcional, `yyyy-MM-dd`).
- **Pré-condições:** papel; formato de `SerialNumber` (400) e de `InstallationDate` (`invalid_installation_date`, 400); local existe (`location_not_found`), modelo existe (`model_not_found`); local ativo (`location_deactivated`), cliente do local ativo (`customer_deactivated`), modelo ativo (`model_deactivated`); série livre **para esse modelo**, entre ativos e desativados (`serial_number_taken`, 409).
- **Fluxo:** 1. Monta os VOs. 2. Carrega o local, o cliente dele e o modelo (pelo `EquipmentModelRepository` do catálogo). 3. `InstalledEquipment.install(location, customer, model, serialNumber, installedOn)`. 4. Confere a série no repository. 5. Salva.
- **Retorno:** `UUIDResponse`. **Endpoint:** `POST /locations/{locationId}/equipment` (201).
- A lista de modelos do formulário é a do catálogo, `GET /catalog/equipment-models?status=ACTIVE`: nenhuma query nova.

### `UpdateInstalledEquipment`

- **Entrada:** ator, `equipmentId`, `serialNumber`, `installedOn`. A série muda para corrigir digitação (ADR-17). Local e modelo **não** vêm: não mudam (ADR-5).
- **Pré-condições:** 1. papel; 2. formato (400, `invalid_installation_date`); 3. equipamento existe (`installed_equipment_not_found`); 4. está ativo (`installed_equipment_deactivated`); 5. série livre para o modelo dele, entre ativos e desativados, fora ele mesmo (`serial_number_taken`). Local, cliente e modelo **não** são conferidos.
- **Fluxo:** carrega, `edit`, confere a série, salva.
- **Endpoint:** `PUT /installed-equipment/{equipmentId}`.

## Desativar e reativar

Seis use cases com a mesma forma: ator, id; papel; carrega; chama o método de domínio; salva. Sem evento, sem confirmação, sem efeito nos filhos (ADR-6).

| Use case | Endpoint | Erros específicos |
|---|---|---|
| `DeactivateCustomer` | `POST /customers/{id}/deactivation` | `customer_not_found`, `customer_deactivated` |
| `ReactivateCustomer` | `POST /customers/{id}/reactivation` | `customer_not_found`, `customer_not_deactivated` |
| `DeactivateLocation` | `POST /locations/{id}/deactivation` | `location_not_found`, `location_deactivated` |
| `ReactivateLocation` | `POST /locations/{id}/reactivation` | `location_not_found`, `location_not_deactivated` |
| `DeactivateInstalledEquipment` | `POST /installed-equipment/{id}/deactivation` | `installed_equipment_not_found`, `installed_equipment_deactivated` |
| `ReactivateInstalledEquipment` | `POST /installed-equipment/{id}/reactivation` | `installed_equipment_not_found`, `installed_equipment_not_deactivated` |

Reativar não confere o pai (como o catálogo, ADR-33): reativar um local de cliente desativado é permitido.

## Queries

Todas `@UseCase(transactional = false)`, abertas aos três papéis. Não carregam aggregate: leem por contrato de leitura em `application.customers.contracts.readers` (`CustomerReader`) e devolvem projeção ([`architecture.md`](../../rules/architecture.md) §7.1). A implementação do reader junta as tabelas do catálogo para os nomes de modelo, fabricante e tipo (mesmo BC, ADR-8). Sem cache.

### `ListCustomers`

- **Entrada:** `text`, `status`, `page`, `size`; o filtro é o `CustomerFilter`.
- **Busca:** `text` casa o nome, o nome fantasia (ADR-21), o documento (sem pontuação), a cidade de um local ativo ou o número de série de um equipamento ativo, sem diferenciar maiúsculas e sem normalizar acento, como o catálogo faz hoje (ADR-13, ADR-15; [`domain-model.md`](domain-model.md#persistência)). É também o destino de "Ver cliente com este documento" (ADR-23), com `status=ALL`. A série acha o cliente mesmo quando o equipamento ativo está num local desativado (a desativação do local não desativa o equipamento, ADR-6): quem digita a série da etiqueta deve achar o cliente. Esse equipamento não entra em `equipmentCount` e a cidade do local desativado não casa; é intencional.
- **Retorno:** `Page<CustomerSummary>`: `id`, `name`, `tradeName` (ou `null`), `document` (normalizado, como o `Document` guarda; a tela formata), `documentType` (`CPF` ou `CNPJ`), `status`, `locationCount` (locais ativos), `equipmentCount` (equipamentos ativos em locais ativos), `cities` (lista de `{city, state}` distintas dos locais ativos, por cidade), `matchedSerials` (até três `{serialNumber, modelName}` dos equipamentos ativos cuja série contém o `text`, por série; vazia sem `text` ou quando o texto não casou série: ADR-27). Sem `email` nem `phone` (ADR-29).
- **Custo:** uma consulta paginada com as contagens por subconsulta, uma consulta das cidades dos ids da página e, só com `text`, uma das séries dos ids da página.
- **Lista vazia:** a tela distingue "nenhum cadastrado" de "todos desativados" com `status=DEACTIVATED&size=1`, só quando a lista padrão volta vazia; a API não muda (ADR-28).
- **Endpoint:** `GET /customers`.

### `GetCustomer`

- **Entrada:** `customerId`. **Erro:** `customer_not_found`.
- **Retorno:** `CustomerDetail`: `id`, `document`, `documentType`, `name`, `tradeName`, `email`, `phone` (os três opcionais como `null`), `status` e **todos** os locais (ativos e desativados), cada um com `id`, `name`, `status`, o endereço (`zipCode`, `street`, `number`, `complement`, `neighborhood`, `city`, `state`) e **todos** os seus equipamentos: `id`, `serialNumber`, `installedOn` (ou `null`), `status`, modelo (`id`, `name`, `status`), fabricante (`id`, `name`), tipo (`id`, `name`) e a característica (`name`, `unit`, `amount`; `null` quando o modelo não tem valor). **Sem paginação** (ADR-22). **Ordem:** locais ativos primeiro, depois por nome; em cada local, equipamentos ativos primeiro, depois por nome do tipo, nome do modelo e série.
- **Endpoint:** `GET /customers/{customerId}`. É a ficha do cliente; a edição de local e de equipamento lê dela, sem `GET` próprio.

## Erros

Contrato público ([`architecture.md`](../../rules/architecture.md) §7.3). Códigos únicos no BC: os do catálogo reaproveitados aqui (`model_not_found`, `model_deactivated`) têm o mesmo significado e a mesma mensagem.

| `code` | Status | Mensagem (pt-BR) | Quando |
|---|---|---|---|
| `forbidden` | 403 | você não tem permissão para isto | Técnico tentando escrever |
| `customer_not_found` | 404 | cliente não encontrado | id inexistente |
| `location_not_found` | 404 | local não encontrado | id inexistente |
| `zip_code_not_found` | 404 | CEP não encontrado | CEP que a BrasilAPI não conhece (ADR-34) |
| `installed_equipment_not_found` | 404 | equipamento não encontrado | id inexistente |
| `model_not_found` | 404 | modelo não encontrado | instalar com modelo inexistente (código do catálogo) |
| `customer_document_taken` | 409 | já existe um cliente com este documento | CPF ou CNPJ repetido, inclusive de cliente desativado |
| `serial_number_taken` | 409 | este modelo já tem um equipamento com esse número de série | série repetida no modelo, inclusive de equipamento desativado |
| `customer_deactivated` | 422 | este cliente está desativado | editar; criar local; instalar em local de cliente desativado; desativar de novo |
| `customer_not_deactivated` | 422 | este cliente não está desativado | reativar quem está ativo |
| `location_deactivated` | 422 | este local está desativado | editar; instalar em local desativado; desativar de novo |
| `location_not_deactivated` | 422 | este local não está desativado | reativar quem está ativo |
| `installed_equipment_deactivated` | 422 | este equipamento está desativado | editar; desativar de novo |
| `installed_equipment_not_deactivated` | 422 | este equipamento não está desativado | reativar quem está ativo |
| `model_deactivated` | 422 | este modelo está desativado | instalar modelo desativado (código do catálogo) |
| `contact_required` | 422 | informe pelo menos um contato: e-mail ou telefone | cadastrar ou editar cliente sem e-mail e sem telefone |
| `trade_name_not_allowed` | 422 | nome fantasia só vale para pessoa jurídica | cadastrar ou editar cliente com CPF e nome fantasia (ADR-21) |
| `invalid_installation_date` | 400 | a data de instalação não pode ser depois de hoje | `InstallationDate` no futuro |
| `concurrent_update` | 409 | o registro foi alterado ao mesmo tempo por outra operação; tente de novo | lock otimista (código e mensagem do kernel) |

Sem código próprio, como 400 com a mensagem do VO: documento inválido (as mensagens do `Document`: "documento inválido: deve ser um cpf ou cnpj válido", "cpf inválido", "cnpj inválido"), e-mail, telefone ("formato de telefone inválido — use E.164 (ex: +5511999999999)"), cada campo do endereço, e os tamanhos de `CustomerName` (2 a 150), `LocationName` (2 a 60) e `SerialNumber` (1 a 60) (ADR-20). O frontend confere CPF e CNPJ (dígitos verificadores: ADR-24), e-mail, CEP (máscara `00000-000`: ADR-19) e telefone (DDD e número, convertido para `+55` em E.164: ADR-18) antes de enviar; o 400 do servidor é a rede de segurança.

### Mensagens da tela

Textos **do frontend** (ADR-31): frase com inicial maiúscula e ponto final, sob o campo, com ícone e texto. A API não produz estes textos e não muda por eles.

**Conferência do formulário, antes de enviar:**

| Campo | Quando | Mensagem |
|---|---|---|
| CPF ou CNPJ | vazio | Informe o CPF ou o CNPJ. |
| CPF ou CNPJ | nem 11 nem 14 caracteres depois de tirar a pontuação | Documento incompleto: o CPF tem 11 dígitos e o CNPJ, 14 caracteres. |
| CPF ou CNPJ | 11 dígitos com dígito verificador errado | CPF inválido: confira os números digitados. |
| CPF ou CNPJ | 14 caracteres com dígito verificador errado | CNPJ inválido: confira os caracteres digitados. |
| Nome (CPF) / Razão social (CNPJ) | vazio | Informe o nome. / Informe a razão social. |
| Nome / Razão social / Nome fantasia | 1 caractere ou mais de 150 | O nome precisa ter de 2 a 150 caracteres. |
| E-mail | formato que o `Email` recusa | E-mail inválido: confira o endereço. |
| Telefone | sem DDD válido ou sem 8 dígitos (fixo) ou 9 começando por 9 (celular) | Telefone inválido: informe DDD e número. |
| E-mail e telefone | os dois vazios | Informe pelo menos um contato: e-mail ou telefone. |
| Nome do local | vazio / 1 caractere ou mais de 60 | Informe o nome do local. / O nome do local precisa ter de 2 a 60 caracteres. |
| CEP | vazio / menos de 8 dígitos | Informe o CEP. / CEP incompleto: informe os 8 dígitos. |
| Logradouro | vazio / 1 caractere ou mais de 150 | Informe o logradouro. / O logradouro precisa ter de 2 a 150 caracteres. |
| Número | vazio | Informe o número (sem número, use S/N). |
| Bairro | vazio / 1 caractere ou mais de 100 | Informe o bairro. / O bairro precisa ter de 2 a 100 caracteres. |
| Cidade | vazio | Informe a cidade. |
| UF | nenhuma escolhida | Escolha a UF. |
| Modelo | nenhum escolhido | Escolha o modelo. |
| Número de série | vazio | Informe o número de série. |
| Data de instalação | depois de hoje | A data de instalação não pode ser depois de hoje. |

Os limites que o campo já impede de passar (`maxLength` de número, complemento, cidade, série e e-mail) não têm mensagem: o campo não aceita o caractere a mais.

**Erro da API com `code`:** a mensagem da tabela de erros, como frase, no lugar abaixo; quando o campo não está na tela, no aviso do formulário.

| `code` | Onde aparece |
|---|---|
| `customer_document_taken` | sob o documento, com a ação "Ver cliente com este documento" (ADR-23) |
| `contact_required` | no topo da seção "Contato", com e-mail e telefone marcados |
| `trade_name_not_allowed` | sob o nome fantasia |
| `serial_number_taken` | sob o número de série; no cadastro, seguido de "Confira a etiqueta ou busque a série na lista de clientes." (tela 18) |
| `invalid_installation_date` | sob a data de instalação |
| `customer_deactivated`, `location_deactivated`, `installed_equipment_deactivated`, `model_deactivated`, `model_not_found`, `*_not_found`, `concurrent_update`, `forbidden` e o 400 sem `code` | no aviso do formulário (ou da ficha) |

## Casos de teste

Além do caminho feliz e de um teste por código de erro da tabela acima, por command e query:

- **Ordem das pré-condições:** Técnico com campo inválido recebe 403 no use case (pela API, a Bean Validation dos requests responde 400 antes de chegar nele, e o Técnico não vê formulário algum); documento repetido com nome inválido recebe 400; documento repetido sem contato recebe 422 (`contact_required`), não 409; cliente desativado editado sem contato recebe `customer_deactivated`.
- **Documento (ADR-3, ADR-12, ADR-24):** "123.456.789-09" e "12345678909" colidem (409); documento de cliente desativado colide; o 409 não traz o documento na mensagem; `PUT /customers/{id}` ignora qualquer documento no corpo e não o altera.
- **Nome fantasia (ADR-21):** CNPJ com nome fantasia cadastra; CPF com nome fantasia recebe `trade_name_not_allowed` no cadastro e na edição; a edição apaga o nome fantasia ausente; a busca acha o cliente pelo trecho do nome fantasia.
- **Telefone (ADR-18):** `+5511999999999` e um E.164 estrangeiro (`+14155552671`) são aceitos pela API; "(11) 99999-9999" é 400. No frontend: máscara de fixo e de celular, envio em `+55`, exibição de volta formatada, número estrangeiro guardado exibido como está.
- **CEP (ADR-19):** "01310-100" e "01310100" gravam `01310100`; "00000-000" é aceito; 7 dígitos é 400.
- **Limites (ADR-20):** nome do cliente com 150 caracteres passa e com 151 é 400; nome do local com 60 passa e com 61 é 400; série com 60 passa e com 61 é 400.
- **Série (ADR-5, ADR-13, ADR-17):** corrigir a série do próprio equipamento grava; trocar só a caixa da própria série grava; série de outro equipamento do mesmo modelo, ativo ou desativado, é 409; a mesma série em outro modelo grava. Busca: acha o cliente pelo trecho da série; não acha pela série de equipamento desativado; série igual em modelos de clientes diferentes devolve os dois; maiúsculas e minúsculas acham igual.
- **Ficha (ADR-22):** traz locais e equipamentos desativados; locais ativos antes dos desativados, depois por nome; equipamentos ativos antes, depois por tipo, modelo e série.
- **Série casada (ADR-27):** buscar "hv22" devolve em `matchedSerials` as séries ativas do cliente que contêm o trecho, com o nome do modelo, no máximo três, em ordem de série; buscar pelo nome devolve `matchedSerials` vazio; série de equipamento desativado não aparece.
- **Lista vazia (ADR-28):** sem cliente nenhum, a tela mostra "Nenhum cliente cadastrado"; com todos desativados, "Nenhum cliente ativo" e "Ver desativados"; a segunda consulta só sai quando a lista padrão volta vazia.
- **Pai desativado (ADR-6, ADR-30):** editar, desativar e reativar o local de cliente desativado grava; editar o equipamento de local desativado grava; cadastrar local em cliente desativado e equipamento em local desativado recusam.
- **Campo opcional (ADR-31):** `tradeName`, `email` ou `phone` `null` é ausência; `"  "` é 400.
- **Mensagens da tela (ADR-31):** cada linha da tabela "Mensagens da tela" tem um teste no frontend, e nenhum envio sai com campo que a tabela recusa.

## Consumo de integration event

Nenhum (ver [`domain-model.md`](domain-model.md#domain-events-e-integration-events)).

## Fora de escopo

- **Apagar** cliente, local ou equipamento: só desativa (ADR-6).
- **Corrigir o documento** de um cliente, nem pelo Administrador (ADR-3, ADR-12); o caminho é prevenir no cadastro (ADR-24). **Mover um equipamento** de local (ADR-14) ou **trocar o modelo** dele (ADR-5).
- **Lista geral de equipamentos** (todos os clientes, por modelo ou por série): a série acha o **cliente**, na busca da lista de clientes (ADR-13).
- **Paginação da ficha** (ADR-22) e **telefone estrangeiro digitável** na tela (ADR-18), com os gatilhos registrados nos ADRs.
- **Contatos múltiplos, garantia, foto, histórico, importação em lote, junção de duplicados** (README da feature, ADR-25).
