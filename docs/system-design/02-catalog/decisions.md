# Decisions: 02-catalog

ADRs **locais desta feature**. Decisão global ou de arquitetura do sistema não entra aqui: arquitetura vai em [`architecture.md`](../../rules/architecture.md), produto/global vai em [`00-overview.md`](../00-overview.md).

## ADR-1: um BC `registry`, com o catálogo como subdomínio em pacote

**Contexto:** o usuário quer o cadastro de fabricante, tipo, modelo e cliente sem duplicar módulo para cada um, e a ligação entre eles (um cliente tem N unidades de um modelo) precisa ser consistente na hora. Com BCs separados, o modelo seria `UUID` cru e a leitura do nome viria de uma cópia local que atrasa segundos: quem cadastra um modelo e já tenta instalá-lo poderia ouvir "modelo não encontrado" ([`architecture.md`](../../rules/architecture.md) §5.3).

**Decisão:** decisão do usuário (opção A): **um BC `registry`**, três módulos (`registry-domain`, `registry-application`, `registry-infrastructure`), com cada **subdomínio como pacote dentro de cada camada** (`catalog` agora, `customers` na feature 03). Critério: **um BC é o menor recorte que precisa ser consistente numa transação**; subdomínios que toleram atraso viram BCs separados. A dependência entre subdomínios é de **mão única**: `customers` enxerga `catalog`, nunca o contrário.

**Consequência:** a ligação cliente → modelo é uma referência comum, gravada na mesma transação. O Maven não guarda a fronteira entre os subdomínios; quem guarda é uma regra de ArchUnit nova (`subdomainsDependOnlyInDeclaredDirection`). A regra geral (subdomínio como pacote, mão única, layout de pacotes) é decisão de arquitetura e mora em [`architecture.md`](../../rules/architecture.md) §4.4, escrita por mim a pedido do usuário. Agrupar subdomínios em módulos Maven aninhados (dois sub-BCs numa pasta) foi descartado: viraria dois BCs com exceção à §4.1.

## ADR-2: tipo é cadastro; enum só onde o código age diferente por valor

**Contexto:** o usuário sugeriu que algumas coisas podem ser enum, e citou o tipo. A empresa trabalha com solar e com ar-condicionado (descoberto no meio da conversa); novos tipos (painel, bateria) são previsíveis.

**Decisão:** decisão do usuário (depois da minha proposta): **fabricante, tipo e modelo são cadastro**. São enum `CatalogStatus` e `MeasurementUnit` (conjuntos fechados: o código compara e converte por valor). Critério: enum quando o código ramifica por valor; cadastro quando o negócio define o conjunto.

**Consequência:** tipo novo é cadastro, sem deploy. O tipo carrega a definição das características, e o código não ramifica por tipo. Reabrir seria um ADR novo.

## ADR-3: o tipo declara as características; o modelo guarda os valores; uma por tipo nesta versão

**Contexto:** potência (kW) no inversor, capacidade (BTU/h) no ar-condicionado, e características que ainda não conhecemos (a lista do dono pode trazer mais). Colunas fixas por tipo exigiriam alterar o código a cada atributo novo.

**Decisão:** decisão do usuário (opção A): `EquipmentType` tem `CharacteristicDefinition` (nome, unidade, obrigatória) e `EquipmentModel` guarda `CharacteristicValue` (id da definição + valor). **Exatamente uma definição por tipo** na primeira versão (*proposta*: começar básico, como o usuário pediu).

**Consequência:** a segunda característica é cadastro novo, sem migração de esquema (a lista e as tabelas já a comportam), e só relaxa a invariante de "uma". A tela e a validação se montam a partir do tipo. Custo: mais regra para escrever do que uma "medida principal" fixa (opção B, descartada).

## ADR-4: fabricante e tipo de um modelo são imutáveis; a unidade trava quando há modelos

**Contexto:** as características de um modelo dependem do tipo dele. Trocar o tipo invalida os valores; trocar a unidade de um tipo com modelos muda o significado dos números já gravados.

**Decisão:** *proposta, aprovada com o mockup:* `manufacturerId` e `typeId` do modelo **não mudam** depois de salvos (errou? cadastra outro e desativa). Em um tipo **com modelos**, a unidade da característica não muda e `required` não passa de `false` para `true` (`characteristic_locked`). O nome da característica e `required` de `true` para `false` mudam sempre.

**Consequência:** nenhum modelo fica inválido depois do cadastro. A trava de `required` é uma **ampliação** do que o mockup mostra (só a unidade aparece travada): a tela precisa desabilitar a caixa "Obrigatória" no mesmo caso, e isso é divergência a sinalizar na comparação mockup × resultado. **Risco aceito:** um modelo criado no mesmo instante em que a unidade muda escapa da trava, porque a criação do modelo só lê o tipo; é edição rara de Administrador, e o prejuízo é um modelo com valor na unidade antiga.

## ADR-5: desativar em vez de apagar, sem confirmação e sem efeito em cascata

**Contexto:** o catálogo é referenciado por equipamentos instalados que vão existir por anos; apagar quebraria a referência. As ações são reversíveis, como no IAM.

**Decisão:** nada se apaga. Fabricante, tipo e modelo se **desativam e reativam**, **sem confirmação** (mesmo critério do [`IAM ADR-11`](../01-iam/decisions.md)). **Desativar não propaga**: desativar um fabricante ou um tipo não desativa os modelos dele. Desativado sai da lista de escolha para **cadastro novo** e não pode ser editado.

**Consequência:** as unidades já instaladas continuam apontando para o modelo. Se um modelo de fabricante desativado ainda pode ser escolhido numa instalação nova é regra da feature 03 ([`open-questions.md`](open-questions.md)).

## ADR-6: quem escreve e quem consulta

**Contexto:** o IAM fixou os papéis Administrador, Comercial e Técnico e deixou cada feature declarar o que cada um pode ([`IAM ADR-5`](../01-iam/decisions.md)).

**Decisão:** *proposta, aprovada com o mockup:* **Administrador e Comercial** cadastram, editam, desativam e reativam; **os três papéis consultam**. O Técnico vê o catálogo sem botões de edição (tela 12 do mockup). A checagem é no use case.

**Consequência:** mudar a regra é mudar o use case, não a tela nem o domínio.

## ADR-7: carga inicial por seeder, só com catálogo vazio, com dados do dono

**Contexto:** o usuário quer o catálogo populado já no primeiro build, com a lista que o dono (irmão do usuário) vai passar. Dado de equipamento errado alimenta proposta e manutenção depois.

**Decisão:** um `CatalogSeeder` (contrato `Seeder<T>` do kernel) lê `seed/catalog.json` e grava **pelo domínio**. Só roda quando **não existe nenhum fabricante nem tipo**. **Nenhuma característica é inventada**: o conteúdo vem da lista do dono.

**Consequência:** reiniciar não recria o que a equipe renomeou ou desativou, e modelo novo depois da carga vai à mão. Enquanto a lista não chega, o arquivo não existe e o seeder não roda (pendência em [`open-questions.md`](open-questions.md)).

## ADR-8: fora desta rodada

**Decisão:** decisão do usuário: **sem histórico de alterações** por enquanto ("é importante, mas vai ter um esforço só para isso quando fizer mais sentido"). Também fora: ficha técnica e foto do modelo, mais de uma característica por tipo, importação por planilha, e a lista atual de clientes (o usuário não sabe onde ela está e não importa agora).

**Consequência:** o histórico, quando vier, é uma feature própria, e o catálogo não carrega trilha de auditoria nesta versão. O `created_at` e o `updated_at` da persistência existem, mas não são histórico.

## ADR-9: limites do valor da característica

**Contexto:** o valor da potência ou da capacidade é número, e o domínio precisa de um limite para recusar lixo (zero, negativo, 12 casas).

**Decisão:** *proposta:* o `CharacteristicAmount` é **maior que zero, com até 3 casas decimais e até 9 dígitos inteiros** (`numeric(12,3)` no banco). Cobre de 0,001 kW a 999.999.999 BTU/h.

**Consequência:** o inversor de 3 kW e o split de 12.000 BTU/h cabem com folga. Se o dono tiver um equipamento com mais precisão, é só mudar o VO e a coluna.

## ADR-10: nome único sem diferenciar maiúsculas

**Contexto:** a equipe cadastra à mão, e "Huawei" e "huawei" seriam o mesmo fabricante cadastrado duas vezes.

**Decisão:** *proposta:* o nome do fabricante e o do tipo são **únicos no catálogo**, e o nome do modelo é **único dentro do fabricante**, sem diferenciar maiúsculas (índice `UNIQUE` sobre `lower(nome)`, traduzido em `ConflictException`).

**Consequência:** o erro "este fabricante já tem um modelo com esse nome" (tela 4 do mockup) vem do banco, sem corrida. Acento não é normalizado ("Inversor" e "Inversór" seriam diferentes): ver [`open-questions.md`](open-questions.md).
