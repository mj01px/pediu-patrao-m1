# API do Dashboard Administrativo

Base: `/api/dashboard` · Somente `GET` · JSON · Exemplos reais, gerados com o perfil `demo`.

## Autenticação e permissão

As rotas aceitam a sessão da tela (cookie) ou **HTTP Basic**, para testar no `curl` ou
no Postman. Só os perfis **ADMIN** e **GERENTE** acessam.

```bash
curl -u gerente:Patrao@2026 "http://localhost:8080/api/dashboard/resumo"
```

Toda resposta de sucesso vem com `Cache-Control: no-store` e um campo `geradoEm`
(hora do servidor no fuso da pizzaria), usado pela tela no "Atualizado às".

## Visão geral

| Método e rota | Requisito | O que devolve |
|---|---|---|
| `GET /api/dashboard/filtros` | RF05 | Opções dos filtros e o período padrão |
| `GET /api/dashboard/resumo` | RF01 | Indicadores gerais e comparação com o período anterior |
| `GET /api/dashboard/vendas` | RF02 | Série de vendas por dia, semana, mês ou hora |
| `GET /api/dashboard/categorias` | RF02 | Vendas por categoria do cardápio |
| `GET /api/dashboard/status` | RF03 | Pedidos por etapa e tempos médios |
| `GET /api/dashboard/produtos/mais-vendidos` | RF04 | Ranking de produtos |

## Filtros comuns (RF05)

Valem para `resumo`, `vendas`, `categorias`, `status` e `produtos/mais-vendidos`.
Todos são opcionais.

| Parâmetro | Tipo | Padrão | Regra |
|---|---|---|---|
| `inicio` | data `yyyy-MM-dd` | `fim` − 29 dias | Inclusivo. Não pode ser depois de `fim`. |
| `fim` | data `yyyy-MM-dd` | hoje | Inclusivo. Período de no máximo 366 dias. |
| `tipo` | texto | todas | Categoria do cardápio (`Pizza`, `Bebida`...), sem diferenciar maiúsculas. Até 40 caracteres. |
| `status` | enum | todas | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO` |

Toda resposta repete o filtro **efetivamente aplicado** (com os padrões resolvidos):

```json
"filtro": { "inicio": "2026-10-12", "fim": "2026-11-10", "dias": 30, "tipo": null, "status": null }
```

Regras dos números: pedido cancelado não entra em faturamento, ticket, itens, séries,
categorias nem ranking; faturamento é líquido (com desconto); com `tipo`, cada item da
categoria entra com o desconto proporcional do pedido. Detalhes em
[REQUISITOS.md](REQUISITOS.md#regras-de-negócio-dos-indicadores).

---

## `GET /api/dashboard/filtros`

Opções para montar os filtros da tela. Sem parâmetros.

**200 OK**

```json
{
  "tipos": ["Bebida", "Esfiha", "Lanche", "Pizza", "Sobremesa"],
  "status": [
    { "valor": "RECEBIDO", "rotulo": "Recebido" },
    { "valor": "EM_PREPARACAO", "rotulo": "Em preparação" },
    { "valor": "PRONTO", "rotulo": "Pronto" },
    { "valor": "SAIU_PARA_ENTREGA", "rotulo": "Saiu para entrega" },
    { "valor": "RETIRADO", "rotulo": "Retirado" },
    { "valor": "FINALIZADO", "rotulo": "Finalizado" },
    { "valor": "CANCELADO", "rotulo": "Cancelado" }
  ],
  "agrupamentos": [
    { "valor": "DIA", "rotulo": "Por dia" },
    { "valor": "SEMANA", "rotulo": "Por semana" },
    { "valor": "MES", "rotulo": "Por mês" },
    { "valor": "HORA", "rotulo": "Por hora do dia" }
  ],
  "ordenacoes": [
    { "valor": "QUANTIDADE", "rotulo": "Quantidade vendida" },
    { "valor": "FATURAMENTO", "rotulo": "Faturamento" }
  ],
  "inicioPadrao": "2026-10-12",
  "fimPadrao": "2026-11-10",
  "limiteDias": 366
}
```

Erros: 401, 403.

---

## `GET /api/dashboard/resumo`

Indicadores gerais do período (RF01) e a comparação com o período imediatamente
anterior, de mesmo tamanho.

**Parâmetros:** filtros comuns.

```bash
curl -u gerente:Patrao@2026 "http://localhost:8080/api/dashboard/resumo?inicio=2026-10-01&fim=2026-10-31&tipo=Pizza"
```

**200 OK**

```json
{
  "filtro": { "inicio": "2026-10-01", "fim": "2026-10-31", "dias": 31, "tipo": "Pizza", "status": null },
  "indicadores": {
    "faturamento": 38353.06,
    "pedidos": 328,
    "pedidosValidos": 309,
    "ticketMedio": 124.12,
    "itensVendidos": 721,
    "cancelados": 19,
    "taxaCancelamento": 5.8,
    "descontoConcedido": 549.34,
    "pedidosComDesconto": 43,
    "emAndamento": 0
  },
  "periodoAnterior": {
    "inicio": "2026-08-31",
    "fim": "2026-09-30",
    "faturamento": 33624.10,
    "pedidosValidos": 280,
    "ticketMedio": 120.09,
    "variacaoFaturamento": 14.1,
    "variacaoPedidos": 10.4,
    "variacaoTicketMedio": 3.4
  },
  "geradoEm": "2026-11-10T14:13:53"
}
```

| Campo | Significado |
|---|---|
| `faturamento` | Soma líquida dos pedidos não cancelados (R$) |
| `pedidos` | Todos os pedidos do filtro, inclusive cancelados |
| `pedidosValidos` | Pedidos não cancelados |
| `ticketMedio` | `faturamento / pedidosValidos` |
| `itensVendidos` | Unidades vendidas em pedidos não cancelados |
| `taxaCancelamento` | `cancelados / pedidos`, em % |
| `descontoConcedido` | Quanto se deixou de faturar com desconto (R$) |
| `emAndamento` | Pedidos não cancelados que ainda não foram finalizados |
| `variacao*` | % sobre o período anterior; `null` quando o anterior é zero |

Erros: 400 (`filtro-invalido`, `formato-invalido`), 401, 403.

---

## `GET /api/dashboard/vendas`

Série temporal de vendas (RF02). Períodos sem venda vêm com zero, para o gráfico não
"pular" dias.

**Parâmetros:** filtros comuns e

| Parâmetro | Tipo | Padrão | Valores |
|---|---|---|---|
| `agrupamento` | enum | `DIA` | `DIA`, `SEMANA` (começa na segunda), `MES`, `HORA` (0h a 23h, somando todos os dias do período) |

```bash
curl -u gerente:Patrao@2026 "http://localhost:8080/api/dashboard/vendas?agrupamento=SEMANA&inicio=2026-10-26&fim=2026-11-10"
```

**200 OK**

```json
{
  "filtro": { "inicio": "2026-10-26", "fim": "2026-11-10", "dias": 16, "tipo": null, "status": null },
  "agrupamento": "SEMANA",
  "pontos": [
    { "chave": "2026-10-26", "rotulo": "Sem. 26/10", "pedidos": 102, "faturamento": 12714.54, "ticketMedio": 124.65 },
    { "chave": "2026-11-02", "rotulo": "Sem. 02/11", "pedidos": 100, "faturamento": 12295.54, "ticketMedio": 122.96 },
    { "chave": "2026-11-09", "rotulo": "Sem. 09/11", "pedidos": 11, "faturamento": 1545.31, "ticketMedio": 140.48 }
  ],
  "geradoEm": "2026-11-10T14:13:53"
}
```

`chave` é estável e ordenável (`2026-11-02` no dia e na semana, `2026-11` no mês, `19`
na hora); `rotulo` é o texto para o eixo.

Erros: 400 (`filtro-invalido`, `formato-invalido` para agrupamento inexistente), 401, 403.

---

## `GET /api/dashboard/categorias`

Faturamento por categoria do cardápio (RF02), do maior para o menor.

**Parâmetros:** filtros comuns.

**200 OK**

```json
{
  "filtro": { "inicio": "2026-11-01", "fim": "2026-11-10", "dias": 10, "tipo": null, "status": null },
  "categorias": [
    { "tipo": "Pizza", "quantidade": 226, "faturamento": 11976.87, "participacao": 75.9 },
    { "tipo": "Sobremesa", "quantidade": 27, "faturamento": 1170.40, "participacao": 7.4 },
    { "tipo": "Esfiha", "quantidade": 152, "faturamento": 1097.93, "participacao": 7.0 },
    { "tipo": "Lanche", "quantidade": 30, "faturamento": 855.60, "participacao": 5.4 },
    { "tipo": "Bebida", "quantidade": 59, "faturamento": 680.25, "participacao": 4.3 }
  ],
  "geradoEm": "2026-11-10T14:13:53"
}
```

`participacao` é o % do faturamento das categorias. Produto excluído do cardápio aparece
como `"Sem categoria"`.

Erros: 400, 401, 403.

---

## `GET /api/dashboard/status`

Situação dos pedidos (RF03): quantos estão em cada etapa e quanto tempo levam.

**Parâmetros:** filtros comuns.

**200 OK**

```json
{
  "filtro": { "inicio": "2026-10-12", "fim": "2026-11-10", "dias": 30, "tipo": null, "status": null },
  "total": 403,
  "etapas": [
    { "status": "RECEBIDO", "rotulo": "Recebido", "quantidade": 0, "percentual": 0.0, "valor": 0.00 },
    { "status": "EM_PREPARACAO", "rotulo": "Em preparação", "quantidade": 1, "percentual": 0.2, "valor": 100.00 },
    { "status": "PRONTO", "rotulo": "Pronto", "quantidade": 0, "percentual": 0.0, "valor": 0.00 },
    { "status": "SAIU_PARA_ENTREGA", "rotulo": "Saiu para entrega", "quantidade": 0, "percentual": 0.0, "valor": 0.00 },
    { "status": "RETIRADO", "rotulo": "Retirado", "quantidade": 0, "percentual": 0.0, "valor": 0.00 },
    { "status": "FINALIZADO", "rotulo": "Finalizado", "quantidade": 382, "percentual": 94.8, "valor": 48428.17 },
    { "status": "CANCELADO", "rotulo": "Cancelado", "quantidade": 20, "percentual": 5.0, "valor": 2588.30 }
  ],
  "tempos": { "preparoMedioMin": 31, "saidaMediaMin": 41, "conclusaoMediaMin": 59 },
  "geradoEm": "2026-11-10T14:13:53"
}
```

- `etapas` traz **sempre** as 7 etapas, na ordem do fluxo, mesmo com zero.
- `valor` é a soma dos pedidos naquela etapa (no cancelado, o valor que deixou de entrar).
- `tempos`: minutos médios da entrada até Pronto, até a saída (Saiu para entrega ou
  Retirado) e até Finalizado, pela linha do tempo do pedido. `null` se nenhum pedido
  chegou à etapa.

Erros: 400, 401, 403.

---

## `GET /api/dashboard/produtos/mais-vendidos`

Ranking de produtos (RF04).

**Parâmetros:** filtros comuns e

| Parâmetro | Tipo | Padrão | Regra |
|---|---|---|---|
| `limite` | inteiro | `10` | De 1 a 50 |
| `ordenarPor` | enum | `QUANTIDADE` | `QUANTIDADE` ou `FATURAMENTO` (o outro critério desempata) |

```bash
curl -u gerente:Patrao@2026 "http://localhost:8080/api/dashboard/produtos/mais-vendidos?limite=3"
```

**200 OK**

```json
{
  "filtro": { "inicio": "2026-10-12", "fim": "2026-11-10", "dias": 30, "tipo": null, "status": null },
  "ordenarPor": "QUANTIDADE",
  "limite": 3,
  "produtos": [
    { "posicao": 1, "produtoId": "6af3506a93d609573fa652d6", "nome": "Esfiha de Frango", "tipo": "Esfiha", "quantidade": 176, "faturamento": 1305.77, "participacao": 2.7 },
    { "posicao": 2, "produtoId": "6af3506a93d609573fa652d0", "nome": "Esfiha de Carne", "tipo": "Esfiha", "quantidade": 175, "faturamento": 1205.40, "participacao": 2.5 },
    { "posicao": 3, "produtoId": "6af3506a93d609573fa652ce", "nome": "Pizza Quatro Queijos", "tipo": "Pizza", "quantidade": 160, "faturamento": 9164.00, "participacao": 18.9 }
  ],
  "geradoEm": "2026-11-10T14:13:53"
}
```

`participacao` é o % do faturamento total do filtro que veio daquele produto. O nome é o
registrado na venda.

Erros: 400 (`filtro-invalido` para limite fora da faixa), 401, 403.

---

## Erros

Formato ProblemDetail (RFC 7807), o mesmo do restante da API, sempre com `codigo`:

```json
{
  "type": "urn:pediupatrao:filtro-invalido",
  "title": "Bad Request",
  "status": 400,
  "detail": "A data inicial (2026-11-10) é posterior à final (2026-11-01).",
  "instance": "/api/dashboard/resumo",
  "codigo": "filtro-invalido",
  "parametro": "inicio"
}
```

| Status | `codigo` | Quando |
|---|---|---|
| 400 | `filtro-invalido` | Período invertido, período maior que 366 dias, categoria com mais de 40 caracteres, `limite` fora de 1 a 50. Traz `parametro` com o campo culpado. |
| 400 | `formato-invalido` | Data fora do formato `yyyy-MM-dd`, `status`, `agrupamento` ou `ordenarPor` inexistente, `limite` não numérico |
| 401 | `nao-autenticado` | Sem login ou senha errada |
| 401 | `sessao-encerrada` | O ADMIN alterou o perfil ou a senha do usuário durante a sessão |
| 403 | `acesso-negado` | Perfil ATENDENTE, ou método diferente de GET |
| 500 | `erro-interno` | Falha inesperada (detalhe só no log do servidor) |
