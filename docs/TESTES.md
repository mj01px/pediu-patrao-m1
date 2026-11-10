# Testes do Dashboard Administrativo

Três níveis de evidência:

1. **Testes automatizados** (JUnit 5 + Mockito + MockMvc), que rodam sem MongoDB.
2. **Requisições reais à API**, com o app rodando sobre o MongoDB, geradas por script.
3. **Roteiro de teste manual da tela**.

## 1. Testes automatizados

```bash
./mvnw test
```

Resultado: **96 testes, 0 falhas** (64 da parte 1 + 32 novos do dashboard).
Saída completa em [evidencias/testes-automatizados.txt](evidencias/testes-automatizados.txt).

| Suíte nova | Testes | O que prova |
|---|:-:|---|
| `CalculadoraDashboardTest` | 13 | As regras dos números, com um cenário calculado à mão (3 pedidos: um finalizado, um com 10% de desconto, um cancelado) |
| `FiltroDashboardTest` | 6 | Período padrão de 30 dias, limites do período, período anterior, recusa de período invertido ou maior que 366 dias |
| `DashboardApiTest` | 9 | Contrato HTTP: 401 sem login, 403 para atendente e para POST, 200 para gerente e admin, filtros chegando ao service, erros 400 no formato ProblemDetail, `Cache-Control: no-store` |
| `DashboardServiceTest` | 4 | `@PreAuthorize` barrando o atendente direto no service, consulta ao banco com o período certo (atual e anterior), fuso da pizzaria, limite do ranking |

### Casos de teste das regras de negócio

Cenário de `CalculadoraDashboardTest` (período 01/10 a 03/10/2026):

| Pedido | Itens | Valor | Situação |
|---|---|---|---|
| P1, 01/10 19h | 2 Calabresa (99,80) + 1 Guaraná (13,00) | 112,80 | Finalizado (pronto em 25 min, saiu em 30, finalizado em 50) |
| P2, 01/10 20h | 1 Calabresa (49,90) com 10% de desconto | 44,91 | Em preparação |
| P3, 03/10 12h | 4 Esfihas | 28,00 | Cancelado |

| ID | Caso | Esperado | Resultado |
|---|---|---|:-:|
| CT01 | Indicadores sem filtro | Faturamento 157,71; 3 pedidos, 2 válidos; ticket 78,86; 4 itens; taxa de cancelamento 33,3%; desconto 4,99 | ✅ |
| CT02 | Filtro `tipo=pizza` (minúsculo) | Faturamento 144,71 (99,80 + 49,90 − 10%); guaraná fora; 2 pedidos | ✅ |
| CT03 | Filtro `status=FINALIZADO` | 1 pedido, 112,80, 0 cancelados | ✅ |
| CT04 | Período sem pedidos | Tudo zero, sem divisão por zero | ✅ |
| CT05 | Série por dia | 3 pontos, dia 02 com zero, dia 03 com zero (só tinha cancelado) | ✅ |
| CT06 | Série por hora | 24 faixas; 19h = 112,80; 20h = 44,91 | ✅ |
| CT07 | Série por semana | Começa na segunda 28/09 | ✅ |
| CT08 | Categorias | Pizza 144,71 (91,8%) antes de Bebida | ✅ |
| CT09 | Etapas | As 7 etapas na ordem do fluxo; só 3 com pedidos | ✅ |
| CT10 | Tempos médios | 25 / 30 / 50 min | ✅ |
| CT11 | Ranking por quantidade e por faturamento | Calabresa em 1º (3 un., 144,71) | ✅ |
| CT12 | Produto excluído do cardápio | Aparece como "Sem categoria" | ✅ |
| CT13 | Variação sem base | `null` quando o período anterior é zero | ✅ |
| CT14 | Sem login | 401 `nao-autenticado` | ✅ |
| CT15 | Atendente | 403 `acesso-negado` na rota e `AccessDeniedException` no service | ✅ |
| CT16 | POST no dashboard | 403 (API somente leitura) | ✅ |
| CT17 | Período invertido | 400 `filtro-invalido`, `parametro: inicio` | ✅ |
| CT18 | Período de 400 dias | 400 `filtro-invalido`; 366 dias é aceito | ✅ |
| CT19 | Data `10/11/2026`, status `VOANDO`, agrupamento `ANO` | 400 `formato-invalido` | ✅ |
| CT20 | Limite 0 ou 51 | 400 `filtro-invalido`; sem limite usa 10 | ✅ |

## 2. Requisições reais à API

Com o app rodando (perfil `demo`, 1.106 pedidos em 90 dias no MongoDB), o script
chama cada endpoint com sucesso e com erro e grava as requisições e respostas:

```bash
BASE=http://localhost:8080 bash docs/evidencias/gerar-evidencias.sh
```

Resultado em [evidencias/requisicoes-api.md](evidencias/requisicoes-api.md):

| Cenário | Status |
|---|:-:|
| Filtros, resumo, resumo por categoria, vendas por semana, vendas por hora, categorias, status, status só cancelados, top 5 por faturamento | 200 |
| Período sem pedidos (dados zerados, sem erro) | 200 |
| Sem login | 401 |
| Atendente | 403 |
| Período invertido, período maior que 366 dias, limite 500 | 400 `filtro-invalido` |
| Data em formato errado, status inexistente | 400 `formato-invalido` |

Conferência cruzada: no resumo dos últimos 30 dias, os pedidos por etapa
(`/status`: 1 em preparação + 382 finalizados + 20 cancelados = 403) batem com
`pedidos: 403` e `cancelados: 20` do `/resumo`.

## 3. Roteiro de teste manual da tela

Rodar com o perfil `demo` e entrar como `gerente`.

| # | Passo | Esperado |
|---|---|---|
| M01 | Entrar como `atendente` | O menu não mostra **Dashboard**; abrir `/dashboard` na mão dá acesso negado |
| M02 | Entrar como `gerente` e abrir **Dashboard** | Cartões, gráfico, situação, categorias e ranking preenchidos com os últimos 30 dias; "Atualizado às hh:mm:ss" |
| M03 | Clicar em **7 dias** | Datas mudam, o botão fica marcado, números e URL mudam |
| M04 | Escolher categoria **Pizza** e aplicar | Ranking só com pizzas; faturamento menor que o total |
| M05 | Trocar o gráfico para **Pedidos** | O gráfico troca a medida sem nova consulta |
| M06 | Trocar o agrupamento para **Hora** | Pico entre 19h e 21h |
| M07 | Ordenar o ranking por **Faturamento** | Pizzas sobem para o topo |
| M08 | Colocar a data inicial depois da final | Mensagem "A data inicial não pode ser depois da final", nada é consultado |
| M09 | Período de 2020 | Cartões zerados, "Nenhuma venda no período", sem erro |
| M10 | Copiar a URL com filtro e abrir em outra aba | Mesmo filtro aplicado |
| M11 | Registrar um pedido em outra aba e esperar 60 s (ou clicar **Atualizar**) | Pedido aparece em "Situação dos pedidos" e no faturamento |
| M12 | Derrubar o servidor e clicar **Atualizar** | Aviso "Sem conexão...", números anteriores continuam na tela; **Tentar de novo** volta ao normal com o servidor no ar |
| M13 | Abrir no celular (ou janela estreita) | Filtros em duas colunas, sem rolagem horizontal da página |
