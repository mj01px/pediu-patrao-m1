# Evidências: requisições reais à API do dashboard

Gerado em 10/11/2026 14:12 por `docs/evidencias/gerar-evidencias.sh`, contra o app rodando
com o perfil `demo` e MongoDB local. Os valores mudam conforme a data em que o script roda.

## Cenários de sucesso

### Opções dos filtros (RF05)

```http
GET /api/dashboard/filtros
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "tipos": [
    "Bebida",
    "Esfiha",
    "Lanche",
    "Pizza",
    "Sobremesa"
  ],
  "status": [
    {
      "valor": "RECEBIDO",
      "rotulo": "Recebido"
    },
    {
      "valor": "EM_PREPARACAO",
      "rotulo": "Em preparação"
    },
    {
      "valor": "PRONTO",
      "rotulo": "Pronto"
    },
    {
      "valor": "SAIU_PARA_ENTREGA",
      "rotulo": "Saiu para entrega"
    },
    {
      "valor": "RETIRADO",
      "rotulo": "Retirado"
    },
    {
      "valor": "FINALIZADO",
      "rotulo": "Finalizado"
    },
    {
      "valor": "CANCELADO",
      "rotulo": "Cancelado"
    }
  ],
  "agrupamentos": [
    {
      "valor": "DIA",
      "rotulo": "Por dia"
    },
    {
      "valor": "SEMANA",
      "rotulo": "Por semana"
    },
    {
      "valor": "MES",
      "rotulo": "Por mês"
    },
    {
      "valor": "HORA",
      "rotulo": "Por hora do dia"
    }
  ],
  "ordenacoes": [
    {
      "valor": "QUANTIDADE",
      "rotulo": "Quantidade vendida"
    },
    {
      "valor": "FATURAMENTO",
      "rotulo": "Faturamento"
    }
  ],
  "inicioPadrao": "2026-10-12",
  "fimPadrao": "2026-11-10",
  "limiteDias": 366
}
```

### Resumo dos últimos 30 dias (RF01)

```http
GET /api/dashboard/resumo
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-10-12",
    "fim": "2026-11-10",
    "dias": 30,
    "tipo": null,
    "status": null
  },
  "indicadores": {
    "faturamento": 48528.17,
    "pedidos": 403,
    "pedidosValidos": 383,
    "ticketMedio": 126.71,
    "itensVendidos": 1523,
    "cancelados": 20,
    "taxaCancelamento": 5.0,
    "descontoConcedido": 732.93,
    "pedidosComDesconto": 60,
    "emAndamento": 1
  },
  "periodoAnterior": {
    "inicio": "2026-09-12",
    "fim": "2026-10-11",
    "faturamento": 42922.98,
    "pedidosValidos": 354,
    "ticketMedio": 121.25,
    "variacaoFaturamento": 13.1,
    "variacaoPedidos": 8.2,
    "variacaoTicketMedio": 4.5
  },
  "geradoEm": "2026-11-10T14:12:32"
}
```

### Resumo filtrado por categoria Pizza (RF01 + RF05)

```http
GET /api/dashboard/resumo?tipo=Pizza
Authorization: Basic (admin)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-10-12",
    "fim": "2026-11-10",
    "dias": 30,
    "tipo": "Pizza",
    "status": null
  },
  "indicadores": {
    "faturamento": 37717.54,
    "pedidos": 327,
    "pedidosValidos": 309,
    "ticketMedio": 122.06,
    "itensVendidos": 710,
    "cancelados": 18,
    "taxaCancelamento": 5.5,
    "descontoConcedido": 611.06,
    "pedidosComDesconto": 49,
    "emAndamento": 1
  },
  "periodoAnterior": {
    "inicio": "2026-09-12",
    "fim": "2026-10-11",
    "faturamento": 33378.86,
    "pedidosValidos": 281,
    "ticketMedio": 118.79,
    "variacaoFaturamento": 13.0,
    "variacaoPedidos": 10.0,
    "variacaoTicketMedio": 2.8
  },
  "geradoEm": "2026-11-10T14:12:32"
}
```

### Vendas por semana (RF02)

```http
GET /api/dashboard/vendas?agrupamento=SEMANA
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-10-12",
    "fim": "2026-11-10",
    "dias": 30,
    "tipo": null,
    "status": null
  },
  "agrupamento": "SEMANA",
  "pontos": [
    {
      "chave": "2026-10-12",
      "rotulo": "Sem. 12/10",
      "pedidos": 84,
      "faturamento": 10114.18,
      "ticketMedio": 120.41
    },
    {
      "chave": "2026-10-19",
      "rotulo": "Sem. 19/10",
      "pedidos": 86,
      "faturamento": 11858.6,
      "ticketMedio": 137.89
    },
    {
      "chave": "2026-10-26",
      "rotulo": "Sem. 26/10",
      "pedidos": 102,
      "faturamento": 12714.54,
      "ticketMedio": 124.65
    },
    {
      "chave": "2026-11-02",
      "rotulo": "Sem. 02/11",
      "pedidos": 100,
      "faturamento": 12295.54,
      "ticketMedio": 122.96
    },
    {
      "chave": "2026-11-09",
      "rotulo": "Sem. 09/11",
      "pedidos": 11,
      "faturamento": 1545.31,
      "ticketMedio": 140.48
    }
  ],
  "geradoEm": "2026-11-10T14:12:32"
}
```

### Vendas por hora do dia, última semana (RF02)

```http
GET /api/dashboard/vendas?agrupamento=HORA&inicio=2026-11-04&fim=2026-11-10
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-11-04",
    "fim": "2026-11-10",
    "dias": 7,
    "tipo": null,
    "status": null
  },
  "agrupamento": "HORA",
  "pontos": [
    {
      "chave": "00",
      "rotulo": "0h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "01",
      "rotulo": "1h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "02",
      "rotulo": "2h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "03",
      "rotulo": "3h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "04",
      "rotulo": "4h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "05",
      "rotulo": "5h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "06",
      "rotulo": "6h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "07",
      "rotulo": "7h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "08",
      "rotulo": "8h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "09",
      "rotulo": "9h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "10",
      "rotulo": "10h",
      "pedidos": 0,
      "faturamento": 0.0,
      "ticketMedio": 0.0
    },
    {
      "chave": "11",
      "rotulo": "11h",
      "pedidos": 4,
      "faturamento": 450.46,
      "ticketMedio": 112.62
    },
    {
      "chave": "12",
      "rotulo": "12h",
      "pedidos": 3,
      "faturamento": 386.51,
      "ticketMedio": 128.84
    },
    {
      "chave": "13",
      "rotulo": "13h",
      "pedidos": 6,
      "faturamento": 648.45,
      "ticketMedio": 108.08
    },
    {
      "chave": "14",
      "rotulo": "14h",
      "pedidos": 2,
      "faturamento": 146.0,
      "ticketMedio": 73.0
    },
    {
      "chave": "15",
      "rotulo": "15h",
      "pedidos": 1,
      "faturamento": 203.3,
      "ticketMedio": 203.3
    },
    {
      "chave": "16",
      "rotulo": "16h",
      "pedidos": 1,
      "faturamento": 201.5,
      "ticketMedio": 201.5
    },
    {
      "chave": "17",
      "rotulo": "17h",
      "pedidos": 3,
      "faturamento": 535.0,
      "ticketMedio": 178.33
    },
    {
      "chave": "18",
      "rotulo": "18h",
      "pedidos": 6,
      "faturamento": 860.91,
      "ticketMedio": 143.49
    },
    {
      "chave": "19",
      "rotulo": "19h",
      "pedidos": 15,
      "faturamento": 1908.13,
      "ticketMedio": 127.21
    },
    {
      "chave": "20",
      "rotulo": "20h",
      "pedidos": 25,
      "faturamento": 3253.61,
      "ticketMedio": 130.14
    },
    {
      "chave": "21",
      "rotulo": "21h",
      "pedidos": 14,
      "faturamento": 1733.21,
      "ticketMedio": 123.8
    },
    {
      "chave": "22",
      "rotulo": "22h",
      "pedidos": 3,
      "faturamento": 402.4,
      "ticketMedio": 134.13
    },
    {
      "chave": "23",
      "rotulo": "23h",
      "pedidos": 7,
      "faturamento": 976.1,
      "ticketMedio": 139.44
    }
  ],
  "geradoEm": "2026-11-10T14:12:32"
}
```

### Vendas por categoria (RF02)

```http
GET /api/dashboard/categorias
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-10-12",
    "fim": "2026-11-10",
    "dias": 30,
    "tipo": null,
    "status": null
  },
  "categorias": [
    {
      "tipo": "Pizza",
      "quantidade": 710,
      "faturamento": 37717.54,
      "participacao": 77.7
    },
    {
      "tipo": "Esfiha",
      "quantidade": 494,
      "faturamento": 3579.55,
      "participacao": 7.4
    },
    {
      "tipo": "Sobremesa",
      "quantidade": 60,
      "faturamento": 2615.8,
      "participacao": 5.4
    },
    {
      "tipo": "Lanche",
      "quantidade": 88,
      "faturamento": 2544.3,
      "participacao": 5.2
    },
    {
      "tipo": "Bebida",
      "quantidade": 171,
      "faturamento": 2071.05,
      "participacao": 4.3
    }
  ],
  "geradoEm": "2026-11-10T14:12:33"
}
```

### Situação dos pedidos (RF03)

```http
GET /api/dashboard/status
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-10-12",
    "fim": "2026-11-10",
    "dias": 30,
    "tipo": null,
    "status": null
  },
  "total": 403,
  "etapas": [
    {
      "status": "RECEBIDO",
      "rotulo": "Recebido",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "EM_PREPARACAO",
      "rotulo": "Em preparação",
      "quantidade": 1,
      "percentual": 0.2,
      "valor": 100.0
    },
    {
      "status": "PRONTO",
      "rotulo": "Pronto",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "SAIU_PARA_ENTREGA",
      "rotulo": "Saiu para entrega",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "RETIRADO",
      "rotulo": "Retirado",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "FINALIZADO",
      "rotulo": "Finalizado",
      "quantidade": 382,
      "percentual": 94.8,
      "valor": 48428.17
    },
    {
      "status": "CANCELADO",
      "rotulo": "Cancelado",
      "quantidade": 20,
      "percentual": 5.0,
      "valor": 2588.3
    }
  ],
  "tempos": {
    "preparoMedioMin": 31,
    "saidaMediaMin": 41,
    "conclusaoMediaMin": 59
  },
  "geradoEm": "2026-11-10T14:12:33"
}
```

### Situação só dos cancelados (RF03 + RF05)

```http
GET /api/dashboard/status?status=CANCELADO
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-10-12",
    "fim": "2026-11-10",
    "dias": 30,
    "tipo": null,
    "status": "CANCELADO"
  },
  "total": 20,
  "etapas": [
    {
      "status": "RECEBIDO",
      "rotulo": "Recebido",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "EM_PREPARACAO",
      "rotulo": "Em preparação",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "PRONTO",
      "rotulo": "Pronto",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "SAIU_PARA_ENTREGA",
      "rotulo": "Saiu para entrega",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "RETIRADO",
      "rotulo": "Retirado",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "FINALIZADO",
      "rotulo": "Finalizado",
      "quantidade": 0,
      "percentual": 0.0,
      "valor": 0.0
    },
    {
      "status": "CANCELADO",
      "rotulo": "Cancelado",
      "quantidade": 20,
      "percentual": 100.0,
      "valor": 2588.3
    }
  ],
  "tempos": {
    "preparoMedioMin": null,
    "saidaMediaMin": null,
    "conclusaoMediaMin": null
  },
  "geradoEm": "2026-11-10T14:12:33"
}
```

### Top 5 produtos por faturamento (RF04)

```http
GET /api/dashboard/produtos/mais-vendidos?limite=5&ordenarPor=FATURAMENTO
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2026-10-12",
    "fim": "2026-11-10",
    "dias": 30,
    "tipo": null,
    "status": null
  },
  "ordenarPor": "FATURAMENTO",
  "limite": 5,
  "produtos": [
    {
      "posicao": 1,
      "produtoId": "6af3506a93d609573fa652ce",
      "nome": "Pizza Quatro Queijos",
      "tipo": "Pizza",
      "quantidade": 160,
      "faturamento": 9164.0,
      "participacao": 18.9
    },
    {
      "posicao": 2,
      "produtoId": "6af3506a93d609573fa652d4",
      "nome": "Pizza Pepperoni",
      "tipo": "Pizza",
      "quantidade": 113,
      "faturamento": 6660.88,
      "participacao": 13.7
    },
    {
      "posicao": 3,
      "produtoId": "6af3506a93d609573fa652d3",
      "nome": "Pizza Portuguesa",
      "tipo": "Pizza",
      "quantidade": 116,
      "faturamento": 6142.5,
      "participacao": 12.7
    },
    {
      "posicao": 4,
      "produtoId": "6af3506a93d609573fa652d2",
      "nome": "Pizza Margherita",
      "tipo": "Pizza",
      "quantidade": 124,
      "faturamento": 5646.5,
      "participacao": 11.6
    },
    {
      "posicao": 5,
      "produtoId": "6af3506a93d609573fa652cd",
      "nome": "Pizza Calabresa",
      "tipo": "Pizza",
      "quantidade": 111,
      "faturamento": 5391.7,
      "participacao": 11.1
    }
  ],
  "geradoEm": "2026-11-10T14:12:33"
}
```

### Período sem pedidos (dados vazios tratados)

```http
GET /api/dashboard/resumo?inicio=2020-01-01&fim=2020-01-31
Authorization: Basic (gerente)
```

**HTTP 200**

```json
{
  "filtro": {
    "inicio": "2020-01-01",
    "fim": "2020-01-31",
    "dias": 31,
    "tipo": null,
    "status": null
  },
  "indicadores": {
    "faturamento": 0.0,
    "pedidos": 0,
    "pedidosValidos": 0,
    "ticketMedio": 0.0,
    "itensVendidos": 0,
    "cancelados": 0,
    "taxaCancelamento": 0.0,
    "descontoConcedido": 0.0,
    "pedidosComDesconto": 0,
    "emAndamento": 0
  },
  "periodoAnterior": {
    "inicio": "2019-12-01",
    "fim": "2019-12-31",
    "faturamento": 0.0,
    "pedidosValidos": 0,
    "ticketMedio": 0.0,
    "variacaoFaturamento": null,
    "variacaoPedidos": null,
    "variacaoTicketMedio": null
  },
  "geradoEm": "2026-11-10T14:12:33"
}
```

## Cenários de erro

### Sem login → 401

```http
GET /api/dashboard/resumo
```

**HTTP 401**

```json
{
  "type": "urn:pediupatrao:nao-autenticado",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Faça login para usar este recurso.",
  "instance": "/api/dashboard/resumo",
  "codigo": "nao-autenticado"
}
```

### Atendente → 403

```http
GET /api/dashboard/resumo
Authorization: Basic (atendente)
```

**HTTP 403**

```json
{
  "type": "urn:pediupatrao:acesso-negado",
  "title": "Forbidden",
  "status": 403,
  "detail": "Seu perfil não pode executar esta operação.",
  "instance": "/api/dashboard/resumo",
  "codigo": "acesso-negado"
}
```

### Período invertido → 400 filtro-invalido

```http
GET /api/dashboard/resumo?inicio=2026-11-10&fim=2026-11-01
Authorization: Basic (gerente)
```

**HTTP 400**

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

### Período maior que 366 dias → 400 filtro-invalido

```http
GET /api/dashboard/vendas?inicio=2024-01-01&fim=2026-01-01
Authorization: Basic (gerente)
```

**HTTP 400**

```json
{
  "type": "urn:pediupatrao:filtro-invalido",
  "title": "Bad Request",
  "status": 400,
  "detail": "O período pode ter no máximo 366 dias.",
  "instance": "/api/dashboard/vendas",
  "codigo": "filtro-invalido",
  "parametro": "fim"
}
```

### Data em formato errado → 400 formato-invalido

```http
GET /api/dashboard/resumo?inicio=10/11/2026
Authorization: Basic (gerente)
```

**HTTP 400**

```json
{
  "type": "urn:pediupatrao:formato-invalido",
  "title": "Bad Request",
  "status": 400,
  "detail": "Formato ou valor inválido na requisição.",
  "instance": "/api/dashboard/resumo",
  "codigo": "formato-invalido"
}
```

### Status inexistente → 400 formato-invalido

```http
GET /api/dashboard/status?status=VOANDO
Authorization: Basic (gerente)
```

**HTTP 400**

```json
{
  "type": "urn:pediupatrao:formato-invalido",
  "title": "Bad Request",
  "status": 400,
  "detail": "Formato ou valor inválido na requisição.",
  "instance": "/api/dashboard/status",
  "codigo": "formato-invalido"
}
```

### Limite fora da faixa → 400 filtro-invalido

```http
GET /api/dashboard/produtos/mais-vendidos?limite=500
Authorization: Basic (gerente)
```

**HTTP 400**

```json
{
  "type": "urn:pediupatrao:filtro-invalido",
  "title": "Bad Request",
  "status": 400,
  "detail": "O limite deve estar entre 1 e 50.",
  "instance": "/api/dashboard/produtos/mais-vendidos",
  "codigo": "filtro-invalido",
  "parametro": "limite"
}
```

