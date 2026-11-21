# Open questions: 02-catalog

Fechadas e migradas: **quem está agindo** (o `Actor` e o `Role` vão para o `shared`, [`architecture.md`](../../rules/architecture.md) §3.1) e **subdomínios dentro de um BC** (§4.4).

## Lista de modelos do dono

A carga inicial (ADR-7) depende da lista que o irmão do usuário passa. Não sei o formato (planilha, foto, texto) nem se traz características além de potência e capacidade. Se trouxer mais, a invariante "uma característica por tipo" (ADR-3) muda antes de a carga ser escrita.

**Fecha com:** a lista chegar. Sem ela, o seeder fica escrito mas sem dados; o catálogo nasce vazio e se cadastra à mão.

## Fabricante desativado: o modelo dele ainda pode ser instalado?

Desativar um fabricante não desativa os modelos (ADR-5). Se um modelo ativo de um fabricante desativado entra na escolha de uma instalação nova é regra do equipamento instalado.

**Fecha com:** a modelagem da feature 03.

## Busca sem acento

A busca ignora maiúsculas, mas não acento ("inversor" acha "Inversor", "conversor" não acha "Convérsor"). O nome único também não normaliza acento (ADR-10). Resolver exigiria a extensão `unaccent` do Postgres ou normalização no domínio.

**Fecha com:** a equipe sentir o problema no uso real; até lá, a busca é por maiúscula e minúscula apenas.

## Casca do celular

O mockup traz **barra de navegação inferior** (Catálogo e Usuários); o app hoje só tem barra superior e menu da conta no celular. É mudança na casca já feita para o IAM. Também fica a ver a barra do Técnico, que hoje teria um item só.

**Fecha com:** o plano do frontend implementar a barra inferior junto das telas do catálogo (o mockup aprovado a inclui); reabrir só se o usuário quiser outra navegação.
