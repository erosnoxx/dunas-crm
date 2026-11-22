# Decisions: 03-customers

ADRs **locais desta feature**. Decisão global ou de arquitetura do sistema não entra aqui: arquitetura vai em [`architecture.md`](../../rules/architecture.md), produto/global vai em [`00-overview.md`](../00-overview.md).

## ADR-1: subdomínio `customers` no BC `registry`

**Contexto:** o catálogo já decidiu que cliente, local e equipamento instalado moram no mesmo BC do modelo, porque a ligação equipamento → modelo precisa ser consistente na hora ([`02-catalog` ADR-1](../02-catalog/decisions.md)). A regra geral de subdomínio está na [`architecture.md`](../../rules/architecture.md) §4.4.

**Decisão:** decisão do usuário: novo subdomínio **`customers`** no BC `registry`, pacote `com.crm.registry.<camada>.customers` nos mesmos três módulos Maven do catálogo. Migrations em `db/migration/registry/`, na próxima versão (`V2__`).

**Consequência:** nenhum módulo novo, nenhum registro novo no `core` nem no `boot`. A direção `registry.customers → registry.catalog` já está declarada em `SUBDOMAIN_DEPENDENCIES` (`BoundedContextRulesTest`); o contrário continua proibido.

## ADR-2: cliente é pessoa física ou jurídica, com um e-mail e um telefone

**Contexto:** a empresa atende casas e empresas. Pedir o tipo junto do documento abriria espaço para os dois discordarem, e o `Document` de `shared-domain` já infere CPF (11 caracteres) ou CNPJ (14) pelo comprimento. Contato é para avisar e agendar visita; a lista de contatos múltiplos foi descartada pelo usuário.

**Decisão:** decisão do usuário: o cliente tem **um documento** (`Document`: CPF ou CNPJ, o tipo vem dele), **um nome** (nome da pessoa ou razão social, no mesmo campo) e **um e-mail e um telefone**, sem lista de contatos. Cada contato, sozinho, é opcional; a regra de que **pelo menos um** dos dois existe é o [ADR-16](#adr-16-pelo-menos-um-contato-por-cliente).

**Consequência:** o formulário é curto e igual para física e jurídica, com o rótulo do nome mudando pelo tipo de documento digitado. Pessoa de contato não existe nesta versão; o nome fantasia opcional da pessoa jurídica é o [ADR-21](#adr-21-nome-fantasia-opcional-para-pessoa-jurídica). A regra "pelo menos um contato" é o ADR-16.

## ADR-3: documento único, comparado normalizado, e imutável

**Contexto:** o mesmo CPF digitado com e sem pontuação não pode virar dois clientes. Um cliente desativado que volta a comprar é o mesmo cliente.

**Decisão:** decisão do usuário: **documento único** no cadastro; o segundo cadastro com o mesmo CPF ou CNPJ é recusado com 409 (`customer_document_taken`), comparando o documento **normalizado** (só letras e dígitos, em maiúsculas, como o `Document` já guarda). *Proposta:* a unicidade cobre os **desativados** (o caminho é reativar), e o documento **não muda** depois do cadastro, nem por correção ([ADR-12](#adr-12-o-documento-do-cliente-é-imutável-sem-command-de-correção)).

**Consequência:** a unicidade é um `UNIQUE (document)` no banco, sem corrida, e o repository traduz a violação no mesmo código. A mensagem de erro não repete o documento (a mensagem vai ao log: [`code-style.md`](../../rules/code-style.md) §5). Mudar a pessoa física para jurídica é cadastro novo. Como a unicidade cobre os desativados, desativar e cadastrar de novo **não** libera um documento digitado errado (ADR-12).

## ADR-4: local é um endereço em campos, digitado à mão

**Contexto:** um cliente instala em mais de um lugar (casa, filial, fazenda), e o equipamento está num lugar, não "no cliente". O `Address` de `shared-domain` já exige CEP, logradouro, número, bairro, cidade e UF, com complemento opcional, sem endereço parcial.

**Decisão:** decisão do usuário: **local** é um endereço onde há instalação; o cliente tem **1..N locais**, e o equipamento se liga ao local. Endereço **em campos** (o `Address` do shared), **sem consulta automática de CEP** por enquanto. *Proposta:* o local tem um **nome curto** (`LocationName`, de 2 a 60 caracteres) para a ficha distinguir "Casa" de "Fazenda" sem ler o endereço inteiro; o cliente pode existir **sem nenhum local** (o cadastro do cliente e o do primeiro local são passos separados).

**Consequência:** o CEP é conferido só no formato (8 dígitos): um CEP que não existe passa. A consulta de CEP, quando vier, é integração com terceiro, atrás de contrato e Translator ([`architecture.md`](../../rules/architecture.md) §4.3), e só preenche o formulário: o domínio não muda.

## ADR-5: equipamento instalado é uma unidade de um modelo, num local, com série única por modelo

**Contexto:** a manutenção e o monitoramento olham a unidade física: o inversor de série X, no telhado Y. Quantidade como número ("3 splits") perde qual unidade é qual.

**Decisão:** decisão do usuário: o equipamento instalado é **modelo do catálogo + local + número de série + data de instalação opcional**, **uma linha por unidade**. A série é **texto livre, única por modelo**. *Proposta:* a unicidade não diferencia maiúsculas e cobre os **desativados**; o **local e o modelo não mudam** depois do cadastro (errou? desativa e cadastra de novo, como o fabricante e o tipo do modelo em [`02-catalog` ADR-4](../02-catalog/decisions.md)); a **série e a data mudam**, para corrigir digitação ([ADR-17](#adr-17-a-série-do-equipamento-muda-para-corrigir-digitação)). A data não pode ser depois de hoje.

**Consequência:** a unicidade é `UNIQUE (model_id, lower(serial_number))`, sem corrida. Como cobre os desativados e o local não muda, uma unidade **retirada de um cliente e reinstalada em outro** não se cadastra de novo: é o caso "mover equipamento", fora desta versão (ADR-14).

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

**Decisão:** decisão do usuário: **sem garantia, sem foto e sem histórico** do equipamento; **sem contatos múltiplos**; **sem consulta de CEP**. Também fora: importação em lote e junção de clientes duplicados (ADR-25), mover equipamento de local (ADR-14), corrigir o documento (ADR-12), a busca sem acento própria do `customers` (ADR-15), e o histórico de alterações do cadastro (como no [`02-catalog` ADR-8](../02-catalog/decisions.md)).

**Consequência:** o `created_at` e o `updated_at` da persistência existem, mas não são histórico nem data de instalação.

## ADR-10: três aggregates, ligados por `UUID`

**Contexto:** cliente, local e equipamento poderiam ser um aggregate só (o cliente com os locais e os equipamentos dentro). As invariantes que cruzam os três são as de unicidade (do banco, entre clientes) e as de "pai ativo".

**Decisão:** *proposta:* **`Customer`, `Location` e `InstalledEquipment` são aggregates próprios**, com `customerId` no local e `locationId` e `modelId` no equipamento; o pai entra por parâmetro na criação do filho. A análise está no [`domain-model.md`](domain-model.md#fronteiras-de-consistência-por-que-três-aggregates).

**Consequência:** cada um tem repository, rota e versão próprios; cadastrar um equipamento não disputa a versão do cliente. A checagem do pai ativo não trava o pai (risco aceito, sem estado inválido possível).

## ADR-11: busca e paginação da lista de clientes

**Contexto:** a equipe procura o cliente pelo nome, pelo documento que ele dita ao telefone ou pela cidade da instalação.

**Decisão:** decisão do usuário: busca por **texto** (nome, documento, cidade) e filtro de **situação**, paginada como o catálogo (`page`, `size` de 20, máximo 100). *Proposta:* o texto casa sem diferenciar maiúsculas; o documento casa sem pontuação (digitar "123.456" acha "12345678909"); a cidade casa a de qualquer local **ativo**; a situação padrão é `ACTIVE`.

**Consequência:** a busca não diferencia acento do mesmo jeito que o catálogo não diferencia (ADR-15). A busca por número de série é o [ADR-13](#adr-13-a-busca-da-lista-de-clientes-também-casa-o-número-de-série).

## ADR-12: o documento do cliente é imutável, sem command de correção

**Contexto:** o documento (CPF ou CNPJ) identifica o cliente e é único no cadastro, inclusive entre desativados (ADR-3). A pergunta era se um documento digitado errado pode ser corrigido, ao menos pelo Administrador.

**Decisão:** decisão do usuário: o documento **não muda**, e **não haverá command de correção**, nem para o Administrador.

**Consequência:** o `UpdateCustomer` não recebe o documento. Como a unicidade cobre os desativados, desativar o cadastro errado e cadastrar de novo **não** é saída para "digitei errado": o cadastro errado continua dono do documento errado, e o cliente certo pode ser cadastrado com o documento certo, mas como cliente novo, sem caminho para levar para ele os locais e os equipamentos do errado. Como o `Document` confere os dígitos verificadores, o erro de digitação que vira outro CPF ou CNPJ válido é raro. *Consequência aceita*; o caminho prático (prevenir no cadastro, sem correção) é o [ADR-24](#adr-24-documento-digitado-errado-prevenção-no-cadastro-sem-correção).

## ADR-13: a busca da lista de clientes também casa o número de série

**Contexto:** o técnico em campo tem a etiqueta do inversor e quer achar o cliente; a busca do ADR-11 não olhava a série.

**Decisão:** decisão do usuário: o texto da busca da lista de clientes **também casa o número de série** dos equipamentos instalados. *Proposta* dos detalhes: casa a série de equipamento **ativo**, por trecho, sem diferenciar maiúsculas (a série é única por modelo, não global, então mais de um cliente pode aparecer).

**Consequência:** o `ListCustomers` e o reader passam a procurar também em `installed_equipment`, por subconsulta `exists` ligada ao cliente pelos locais (o equipamento não guarda o cliente: ADR-5). O índice único `(model_id, lower(serial_number))` não serve à busca por trecho, então a subconsulta varre a tabela; a lista de uma empresa só é pequena, e isso é aceito. Muda a tela da lista: o campo de busca avisa que aceita série (ver [`design/`](design/README.md)).

## ADR-14: mover equipamento fica fora desta versão

**Contexto:** o local e o modelo do equipamento não mudam, e a série é única mesmo entre desativados (ADR-5). Uma unidade retirada de um cliente e instalada em outro, ou trocada de local, não tem caminho: o cadastro novo bate na série.

**Decisão:** decisão do usuário: **fica para quando o caso aparecer**. Fora de escopo desta versão; sem `MoveInstalledEquipment`.

**Consequência:** nenhuma mudança no domínio nem nas telas. Quando vier, a direção registrada é um command que troca o `locationId`, exigindo o novo local e o cliente dele ativos, sem abrir a unicidade da série só para os ativos (perderia a garantia de que a série identifica uma unidade); vira ADR novo e muda o ADR-5.

## ADR-15: busca sem acento acompanha a solução do catálogo

**Contexto:** a busca por texto ignora maiúsculas, mas não acento ("sao paulo" não acha "São Paulo"). O catálogo tem a mesma pendência ([`02-catalog` open-questions](../02-catalog/open-questions.md)), e na busca por cidade ela pesa mais.

**Decisão:** decisão do usuário: **resolver junto com o catálogo**, nos dois subdomínios de uma vez, em feature ou ajuste próprio. Esta spec não escolhe a solução.

**Consequência:** **esta versão busca como o catálogo faz hoje**: `lower(campo) like :text escape '\\'`, com o texto digitado em minúsculas e cercado de `%` (`CatalogQueries.containing`, nos readers do catálogo em `registry-infrastructure`), sem normalizar acento. A mudança futura é compartilhada, e o `customers` não cria solução local.

## ADR-16: pelo menos um contato por cliente

**Contexto:** o ADR-2 deixava e-mail e telefone opcionais, com o aviso de que a agenda de manutenção podia pedir um contato garantido. Um cadastro sem nenhum jeito de falar com o cliente não serve a ela.

**Decisão:** decisão do usuário: o cliente precisa de **pelo menos um** dos dois, e-mail **ou** telefone. Cada um, sozinho, continua opcional.

**Consequência:** a invariante está no aggregate `Customer` (`register` e `edit`), com o código `contact_required` (422: é regra do aggregate sobre dois campos já válidos, como `characteristic_required` no catálogo, e não formato de VO, que é 400). Editar um cliente não pode apagar o último contato. O formulário indica que um dos dois é obrigatório e mostra o erro correspondente.

## ADR-17: a série do equipamento muda, para corrigir digitação

**Contexto:** a série é copiada à mão da etiqueta, e errar um caractere é comum. Como a unicidade por modelo cobre os desativados (ADR-5), a alternativa "imutável: desativa e cadastra de novo" deixaria a série errada ocupada para sempre. Não há histórico de alterações (ADR-9).

**Decisão:** decisão delegada pelo usuário (2026-11-22): a **série e a data de instalação mudam** pelo `UpdateInstalledEquipment`, só com o equipamento ativo, por Administrador ou Comercial, com a mesma checagem de unicidade por modelo (fora o próprio equipamento). O local e o modelo continuam imutáveis (ADR-5).

**Consequência:** "uma série identifica uma unidade de um modelo" vale sempre, porque o índice `UNIQUE (model_id, lower(serial_number))` confere toda gravação. Sem histórico, a série anterior se perde: risco aceito, como em todo campo editável desta versão, até o histórico existir (ADR-9). Trocar a série não move equipamento: local e modelo não mudam (ADR-14).

## ADR-18: telefone digitado com máscara brasileira, guardado em E.164

**Contexto:** o `Phone` de `shared-domain` aceita só E.164 (`+5511999999999`), e a equipe digita "(11) 99999-9999". A clientela é brasileira, mas o VO compartilhado não deve ganhar regra de um país: o domínio é neutro e o VO serve a outros BCs.

**Decisão:** decisão delegada pelo usuário (2026-11-22): o formulário tem **máscara brasileira**, DDD e número (8 dígitos para fixo, 9 começando por 9 para celular), e o frontend envia `+55` + DDD + número. **Número estrangeiro não é digitável na tela** nesta versão; a API aceita qualquer E.164 válido, porque o domínio não muda. A exibição formata o `+55` de volta ("(11) 99999-9999") e mostra qualquer outro número como está guardado.

**Consequência:** nenhuma mudança no `Phone` nem no backend; o formato brasileiro mora só no frontend (máscara, conferência e exibição). A tela mostra a própria mensagem ("telefone inválido: informe DDD e número"), e o 400 do servidor é rede de segurança. Gatilho para mudar: o primeiro cliente com telefone estrangeiro, que pede um seletor de país no campo, ainda só no frontend.

## ADR-19: CEP conferido só no formato

**Contexto:** o `ZipCode` remove o que não é dígito e exige 8, sem saber se o CEP existe: "00000-000" passa. A consulta automática de CEP ficou fora (ADR-4, ADR-9).

**Decisão:** decisão delegada pelo usuário (2026-11-22): **só formato** nesta versão. O formulário tem máscara `00000-000` e exige 8 dígitos; o servidor confere o mesmo pelo `ZipCode`. Nenhuma lista de CEPs nem faixa por UF.

**Consequência:** um CEP que não existe passa, e isso é aceito: quem digita tem o endereço na frente, e conferir a existência exigiria a base dos Correios, ou seja, integração com terceiro. Quando a consulta vier, ela fica atrás de contrato e Translator ([`architecture.md`](../../rules/architecture.md) §4.3) e só preenche o formulário; o domínio não muda.

## ADR-20: limites do nome do cliente, do nome do local e da série

**Contexto:** os VOs próprios do `customers` precisavam de limites. O leiaute público do CNPJ da Receita reserva 150 caracteres para a razão social, e uma razão social copiada do cartão CNPJ precisa caber. Série de inversor e de ar-condicionado costuma ter de 10 a 25 caracteres.

**Decisão:** decisão delegada pelo usuário (2026-11-22): `CustomerName` de **2 a 150** caracteres (sobe dos 120 propostos, para caber qualquer razão social); `LocationName` de **2 a 60** e `SerialNumber` de **1 a 60**, confirmados.

**Consequência:** `customers.name` e `customers.trade_name` (ADR-21) são `varchar(150)`, o mesmo limite do `Street`. Sessenta caracteres no local cabem um rótulo como "Fazenda Boa Vista, galpão 2", e na série dão mais que o dobro da maior conhecida; se a lista do dono trouxer série maior, mudam o VO e a coluna.

## ADR-21: nome fantasia opcional para pessoa jurídica

**Contexto:** a equipe conhece a empresa pelo nome fantasia ("Padaria Boa Vista"), não pela razão social ("J. SILVA COMÉRCIO DE ALIMENTOS LTDA"), e procura por ele. O ADR-2 fixou um nome por cliente, no mesmo campo para física e jurídica, e deixou o fantasia em aberto.

**Decisão:** decisão delegada pelo usuário (2026-11-22): o cliente ganha **`tradeName` opcional, só para pessoa jurídica** (CNPJ), com o mesmo VO `CustomerName` (2 a 150). Cliente com CPF e nome fantasia é recusado pelo aggregate (`trade_name_not_allowed`, 422, no `register` e no `edit`). A busca da lista casa também o nome fantasia, e a lista e a ficha mostram os dois. Pessoa de contato continua fora (ADR-2).

**Consequência:** o formulário mostra "Nome fantasia (opcional)" só quando o documento digitado é CNPJ. A lista continua ordenada pelo `name`, para não haver duas ordens; procurar pelo fantasia é papel da busca. Coluna `trade_name varchar(150)` nulável. Como o documento não muda (ADR-12), um cliente nunca passa de jurídica para física com o fantasia preenchido.

## ADR-22: ficha sem paginação, um bloco por local

**Contexto:** a ficha (`GetCustomer`) traz o cliente com todos os locais e todos os equipamentos. Contagem esperada: uma casa tem um local e de 1 a 5 equipamentos; uma empresa, alguns locais e dezenas de equipamentos; mesmo com placas cadastradas uma a uma (ADR-5), uma usina comercial fica em poucas centenas.

**Decisão:** decisão delegada pelo usuário (2026-11-22): **sem paginação**, numa resposta só. Locais com os **ativos primeiro**, depois por nome; os equipamentos de cada local com os ativos primeiro, depois por tipo, modelo e série. Na tela, **um bloco por local** (nome, endereço, situação e número de equipamentos ativos) com os equipamentos dentro: na web, um cartão com tabela, todos abertos; no celular, uma seção recolhível, aberta se o local está ativo e fechada se está desativado. Desativados aparecem marcados por forma e texto, nunca escondidos.

**Consequência:** a edição de local e de equipamento continua lendo da ficha, sem `GET` próprio. Gatilho para paginar: um cliente com mais de 300 equipamentos, ou uma ficha que demore a abrir; os equipamentos de cada local passariam a vir de um `GET /locations/{id}/equipment` paginado, sem mudança no domínio.

## ADR-23: documento repetido leva ao cliente que já existe

**Contexto:** o cadastro com documento já usado é recusado (`customer_document_taken`), inclusive o de cliente desativado, e o caminho certo é abrir e, se for o caso, reativar o cadastro existente (ADR-3). A resposta do erro não traz o id dele.

**Decisão:** decisão delegada pelo usuário (2026-11-22): o erro no formulário tem a ação **"Ver cliente com este documento"**, que abre a lista de clientes buscando o documento digitado, com a situação "todos". A resposta do 409 **não muda**.

**Consequência:** nenhuma mudança no backend: a busca por documento já existe (ADR-11) e quem pode cadastrar pode consultar (ADR-7). A mensagem continua sem o documento (ADR-3).

## ADR-24: documento digitado errado: prevenção no cadastro, sem correção

**Contexto:** o documento é imutável e não há command de correção, nem para o Administrador (ADR-12). Faltava o caminho prático. Um command de correção, mesmo só do Administrador, reabriria o ADR-12. Desativar o errado e recadastrar deixa o documento errado ocupado (ADR-3) e, se já houver equipamentos, as séries deles presas ao cadastro errado (ADR-5).

**Decisão:** decisão delegada pelo usuário (2026-11-22), dentro do ADR-12: **nenhum command de correção**; o esforço vai para **evitar** o erro. O cadastro mostra, ao digitar, o tipo reconhecido (CPF ou CNPJ) e o documento formatado, confere os dígitos verificadores antes de enviar e avisa no campo que **o documento não pode ser alterado depois de salvo**. Na edição, o documento é somente leitura, com o mesmo aviso. Erro notado antes de cadastrar locais e equipamentos (o caso comum: é logo depois do cadastro) se resolve desativando o errado e cadastrando o certo.

**Consequência:** o documento errado fica ocupado pelo cadastro desativado, o que só incomoda se for o documento real de outra pessoa que um dia vire cliente (dois acasos: dígitos verificadores válidos por engano e essa pessoa comprar). Erro notado depois de instalar equipamentos fica sem saída nesta versão. Gatilho: o primeiro caso real; a saída então é transferir locais e equipamentos entre clientes, decidida com o usuário junto do ADR-14, porque reabre o ADR-12 e a imutabilidade do local.

## ADR-25: importação em lote e junção de duplicados ficam fora

**Contexto:** a lista atual de clientes não está disponível ([`02-catalog` ADR-8](../02-catalog/decisions.md)). O documento único impede o duplicado pelo mesmo CPF ou CNPJ; sobra o duplicado por documento digitado errado (ADR-24).

**Decisão:** decisão delegada pelo usuário (2026-11-22): as duas ficam **fora desta versão**; o cadastro é à mão.

**Consequência:** gatilhos: a **importação** entra quando a lista aparecer, como carga única pelo domínio, com as mesmas regras de documento, contato e série (seeder ou planilha, decidido com o arquivo na mão, como o [`02-catalog` ADR-7](../02-catalog/decisions.md)); a **junção** entra com o primeiro cliente duplicado que já tenha locais ou equipamentos, junto da transferência do ADR-24.

## ADR-26: "Clientes" é o primeiro item da navegação

**Contexto:** o cliente é uma seção nova e vai ser a mais usada: o comercial abre a ficha para atender, e o técnico procura o cliente pela série em campo (ADR-13).

**Decisão:** decisão delegada pelo usuário (2026-11-22): item **"Clientes" primeiro** no menu da web e na barra inferior do celular, antes de Catálogo e Usuários.

**Consequência:** a barra inferior do Técnico passa a ter dois itens, Clientes e Catálogo. O ícone e o rótulo fecham com o mockup.
