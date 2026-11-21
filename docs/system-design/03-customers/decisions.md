# Decisions: 03-customers

ADRs **locais desta feature**. Decisão global ou de arquitetura do sistema não entra aqui: arquitetura vai em [`architecture.md`](../../rules/architecture.md), produto/global vai em [`00-overview.md`](../00-overview.md).

## ADR-1: subdomínio `customers` no BC `registry`

**Contexto:** o catálogo já decidiu que cliente, local e equipamento instalado moram no mesmo BC do modelo, porque a ligação equipamento → modelo precisa ser consistente na hora ([`02-catalog` ADR-1](../02-catalog/decisions.md)). A regra geral de subdomínio está na [`architecture.md`](../../rules/architecture.md) §4.4.

**Decisão:** decisão do usuário: novo subdomínio **`customers`** no BC `registry`, pacote `com.crm.registry.<camada>.customers` nos mesmos três módulos Maven do catálogo. Migrations em `db/migration/registry/`, na próxima versão (`V2__`).

**Consequência:** nenhum módulo novo, nenhum registro novo no `core` nem no `boot`. A direção `registry.customers → registry.catalog` já está declarada em `SUBDOMAIN_DEPENDENCIES` (`BoundedContextRulesTest`); o contrário continua proibido.

## ADR-2: cliente é pessoa física ou jurídica, com um e-mail e um telefone opcionais

**Contexto:** a empresa atende casas e empresas. Pedir o tipo junto do documento abriria espaço para os dois discordarem, e o `Document` de `shared-domain` já infere CPF (11 caracteres) ou CNPJ (14) pelo comprimento. Contato é para avisar e agendar visita; a lista de contatos múltiplos foi descartada pelo usuário.

**Decisão:** decisão do usuário: o cliente tem **um documento** (`Document`: CPF ou CNPJ, o tipo vem dele), **um nome** (nome da pessoa ou razão social, no mesmo campo) e **um e-mail e um telefone**, sem lista de contatos. *Proposta:* **os dois contatos são opcionais**, e nenhum é obrigatório quando o outro falta. Motivo: o cadastro de clientes antigos (instalações feitas antes do sistema) vem muitas vezes sem e-mail, e recusar o cadastro por falta de contato empurra a equipe a inventar um.

**Consequência:** o formulário é curto e igual para física e jurídica, com o rótulo do nome mudando pelo tipo de documento digitado. Nome fantasia e pessoa de contato não existem nesta versão ([`open-questions.md`](open-questions.md)). Se a agenda de manutenção precisar de um contato garantido, a regra "pelo menos um" entra aqui como ADR novo.

## ADR-3: documento único, comparado normalizado, e imutável

**Contexto:** o mesmo CPF digitado com e sem pontuação não pode virar dois clientes. Um cliente desativado que volta a comprar é o mesmo cliente.

**Decisão:** decisão do usuário: **documento único** no cadastro; o segundo cadastro com o mesmo CPF ou CNPJ é recusado com 409 (`customer_document_taken`), comparando o documento **normalizado** (só letras e dígitos, em maiúsculas, como o `Document` já guarda). *Proposta:* a unicidade cobre os **desativados** (o caminho é reativar), e o documento **não muda** depois do cadastro (errou? desativa e cadastra de novo; pendência em [`open-questions.md`](open-questions.md)).

**Consequência:** a unicidade é um `UNIQUE (document)` no banco, sem corrida, e o repository traduz a violação no mesmo código. A mensagem de erro não repete o documento (a mensagem vai ao log: [`code-style.md`](../../rules/code-style.md) §5). Mudar a pessoa física para jurídica é cadastro novo.

## ADR-4: local é um endereço em campos, digitado à mão

**Contexto:** um cliente instala em mais de um lugar (casa, filial, fazenda), e o equipamento está num lugar, não "no cliente". O `Address` de `shared-domain` já exige CEP, logradouro, número, bairro, cidade e UF, com complemento opcional, sem endereço parcial.

**Decisão:** decisão do usuário: **local** é um endereço onde há instalação; o cliente tem **1..N locais**, e o equipamento se liga ao local. Endereço **em campos** (o `Address` do shared), **sem consulta automática de CEP** por enquanto. *Proposta:* o local tem um **nome curto** (`LocationName`, de 2 a 60 caracteres) para a ficha distinguir "Casa" de "Fazenda" sem ler o endereço inteiro; o cliente pode existir **sem nenhum local** (o cadastro do cliente e o do primeiro local são passos separados).

**Consequência:** o CEP é conferido só no formato (8 dígitos): um CEP que não existe passa. A consulta de CEP, quando vier, é integração com terceiro, atrás de contrato e Translator ([`architecture.md`](../../rules/architecture.md) §4.3), e só preenche o formulário: o domínio não muda.

## ADR-5: equipamento instalado é uma unidade de um modelo, num local, com série única por modelo

**Contexto:** a manutenção e o monitoramento olham a unidade física: o inversor de série X, no telhado Y. Quantidade como número ("3 splits") perde qual unidade é qual.

**Decisão:** decisão do usuário: o equipamento instalado é **modelo do catálogo + local + número de série + data de instalação opcional**, **uma linha por unidade**. A série é **texto livre, única por modelo**. *Proposta:* a unicidade não diferencia maiúsculas e cobre os **desativados**; o **local e o modelo não mudam** depois do cadastro (errou? desativa e cadastra de novo, como o fabricante e o tipo do modelo em [`02-catalog` ADR-4](../02-catalog/decisions.md)); a **série e a data mudam**, para corrigir digitação (pendência em [`open-questions.md`](open-questions.md)). A data não pode ser depois de hoje.

**Consequência:** a unicidade é `UNIQUE (model_id, lower(serial_number))`, sem corrida. Como cobre os desativados e o local não muda, uma unidade **retirada de um cliente e reinstalada em outro** não se cadastra de novo: é o caso "mover equipamento", em [`open-questions.md`](open-questions.md).

## ADR-6: desativar sem apagar, sem confirmação e sem propagação; o pai desativado só bloqueia filho novo

**Contexto:** o cliente, o local e o equipamento vão ser referenciados por manutenção e monitoramento; apagar quebra a referência. O catálogo fixou "desativar não propaga" e "o pai desativado só bloqueia cadastro novo" ([`02-catalog` ADR-5](../02-catalog/decisions.md)).

**Decisão:** decisão do usuário: cliente, local e equipamento se **desativam e reativam**; nada se apaga; **sem confirmação**; desativar **não propaga** (desativar o cliente não desativa os locais; desativar o local não desativa os equipamentos). *Proposta*, alinhada ao catálogo:
- **Local novo** exige o cliente ativo (`customer_deactivated`).
- **Equipamento novo** exige o local ativo, **o cliente do local ativo** e o modelo ativo (`location_deactivated`, `customer_deactivated`, `model_deactivated`). O cliente é conferido porque, sem propagação, o local de um cliente desativado continua ativo, e conferir só o local deixaria instalar para quem não é mais cliente.
- O **fabricante e o tipo** do modelo **não** são conferidos: desativá-los não desativa o modelo, e o modelo ativo continua instalável. Isso fecha a pendência "Fabricante desativado: o modelo dele ainda pode ser instalado?" do [`open-questions.md` do catálogo](../02-catalog/open-questions.md) quando esta spec for aprovada.
- **Editar e reativar** olham só o próprio aggregate: o local de um cliente desativado se edita e se reativa; o equipamento de modelo desativado também.

**Consequência:** a ficha de um cliente desativado mostra os locais e os equipamentos como estão, e a tela não oferece "Novo local" nem "Novo equipamento" ali. Nenhum estado fica inválido por corrida (ver [`domain-model.md`](domain-model.md#fronteiras-de-consistência-por-que-três-aggregates)).

## ADR-7: quem escreve e quem consulta

**Contexto:** o IAM fixou os papéis e deixou cada feature declarar o que cada um pode; o catálogo escolheu Administrador e Comercial para escrever ([`02-catalog` ADR-6](../02-catalog/decisions.md)).

**Decisão:** decisão do usuário: **Administrador e Comercial** cadastram, editam, desativam e reativam cliente, local e equipamento; o **Técnico só consulta**. Mesmo `Actor` e `Role` de `shared`, checagem no use case, numa classe `CustomerAccess` do subdomínio.

**Consequência:** o Técnico vê a lista e a ficha sem botões de edição. Se o Técnico precisar registrar a série de um equipamento em campo, a regra muda no use case, não na tela nem no domínio.

## ADR-8: o que o `customers` usa do `catalog`

**Contexto:** a regra `subdomainsDependOnlyInDeclaredDirection` permite `registry.customers → registry.catalog` em **qualquer camada** (o pacote verificado é `com.crm.registry.*.catalog..`), e a [`architecture.md`](../../rules/architecture.md) §4.4 diz que subdomínios do mesmo BC têm "referência comum, sem projeção nem evento", com FK permitida. A §6.1 diz que aggregates se referenciam por `UUID` e que a regra que liga dois aggregates mora no domínio.

**Decisão:** *proposta*, dentro da regra existente (nenhuma regra de arquitetura nova):
- **Domínio:** `InstalledEquipment` guarda `modelId` (`UUID`) e recebe o `EquipmentModel` **por parâmetro** em `install`, para conferir `isActive()`. Nenhum outro tipo do catálogo entra no domínio do `customers`.
- **Application:** `InstallEquipment` carrega o modelo pelo `EquipmentModelRepository` do catálogo e reaproveita os códigos `model_not_found` e `model_deactivated` (mesmo BC, mesmo significado).
- **Infrastructure:** `installed_equipment.model_id` tem FK para `equipment_models`; o reader da ficha junta as tabelas do catálogo para os nomes de modelo, fabricante e tipo e o valor da característica.
- **Web e frontend:** o formulário de equipamento usa a lista do catálogo (`GET /catalog/equipment-models?status=ACTIVE`).

**Consequência:** a ligação equipamento → modelo é verificada na mesma transação, sem cópia local. O catálogo continua sem enxergar cliente, então "quantas unidades usam este modelo" é pergunta do `customers`, nunca do catálogo. Uma classe de `web` que injetasse use cases de `customers` e de `catalog` juntos é permitida (é o mesmo BC), mas o formulário não precisa disso.

## ADR-9: fora desta rodada

**Decisão:** decisão do usuário: **sem garantia, sem foto e sem histórico** do equipamento; **sem contatos múltiplos**; **sem consulta de CEP**. Também fora: importação em lote e junção de clientes duplicados (pendências em [`open-questions.md`](open-questions.md)), e o histórico de alterações do cadastro (como no [`02-catalog` ADR-8](../02-catalog/decisions.md)).

**Consequência:** o `created_at` e o `updated_at` da persistência existem, mas não são histórico nem data de instalação.

## ADR-10: três aggregates, ligados por `UUID`

**Contexto:** cliente, local e equipamento poderiam ser um aggregate só (o cliente com os locais e os equipamentos dentro). As invariantes que cruzam os três são as de unicidade (do banco, entre clientes) e as de "pai ativo".

**Decisão:** *proposta:* **`Customer`, `Location` e `InstalledEquipment` são aggregates próprios**, com `customerId` no local e `locationId` e `modelId` no equipamento; o pai entra por parâmetro na criação do filho. A análise está no [`domain-model.md`](domain-model.md#fronteiras-de-consistência-por-que-três-aggregates).

**Consequência:** cada um tem repository, rota e versão próprios; cadastrar um equipamento não disputa a versão do cliente. A checagem do pai ativo não trava o pai (risco aceito, sem estado inválido possível).

## ADR-11: busca e paginação da lista de clientes

**Contexto:** a equipe procura o cliente pelo nome, pelo documento que ele dita ao telefone ou pela cidade da instalação.

**Decisão:** decisão do usuário: busca por **texto** (nome, documento, cidade) e filtro de **situação**, paginada como o catálogo (`page`, `size` de 20, máximo 100). *Proposta:* o texto casa sem diferenciar maiúsculas; o documento casa sem pontuação (digitar "123.456" acha "12345678909"); a cidade casa a de qualquer local **ativo**; a situação padrão é `ACTIVE`.

**Consequência:** a busca não diferencia acento do mesmo jeito que o catálogo não diferencia ([`02-catalog` open-questions](../02-catalog/open-questions.md)). Busca por número de série fica em [`open-questions.md`](open-questions.md).
