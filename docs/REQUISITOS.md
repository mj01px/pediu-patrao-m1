# Dashboard Administrativo: requisitos e decisões de projeto

Segunda parte do **PediuPatrão**. O dashboard dá ao responsável pelo estabelecimento
uma visão do desempenho das vendas, da situação dos pedidos e dos produtos que mais
saem, a partir dos pedidos que o próprio sistema já registra.

- Tela: `GET /dashboard` (menu **Dashboard**), para ADMIN e GERENTE.
- API: `GET /api/dashboard/**`, documentada em [API-DASHBOARD.md](API-DASHBOARD.md).
- Testes e evidências: [TESTES.md](TESTES.md).

## 1. Atores

| Ator | Usa o dashboard? | Por quê |
|---|:-:|---|
| ADMIN (dono) | ✅ | Acompanha o negócio e decide preço, cardápio e equipe |
| GERENTE | ✅ | Acompanha o turno e o mês, dá desconto e cancela |
| ATENDENTE | ❌ | Opera o balcão; faturamento não é informação da função dele |

## 2. Requisitos funcionais

| ID | Requisito | Como foi atendido |
|---|---|---|
| **RF01** | Indicadores gerais de vendas | Seis cartões: faturamento líquido, pedidos válidos, ticket médio, itens vendidos, cancelamentos (com taxa) e descontos concedidos. Os três primeiros mostram a variação contra o período anterior de mesmo tamanho. Endpoint `GET /api/dashboard/resumo`. |
| **RF02** | Análise de vendas (gráficos) | Gráfico de barras de faturamento ou de pedidos, agrupado por dia, semana, mês ou hora do dia (`GET /api/dashboard/vendas`), e barras de participação por categoria do cardápio (`GET /api/dashboard/categorias`). Cada gráfico tem a tabela equivalente ("Ver os dados em tabela"). |
| **RF03** | Situação dos pedidos | Quantidade e percentual por etapa (Recebido até Finalizado, e Cancelado), com as cores do quadro de pedidos, e tempos médios até ficar pronto, até sair e até finalizar, calculados pela linha do tempo do pedido. Endpoint `GET /api/dashboard/status`. |
| **RF04** | Produtos mais vendidos | Ranking com posição, produto, categoria, quantidade, faturamento e participação, ordenável por quantidade ou faturamento, com limite configurável (1 a 50). Endpoint `GET /api/dashboard/produtos/mais-vendidos`. |
| **RF05** | Filtros de consulta | Período (data inicial e final, mais atalhos Hoje, 7, 30 e 90 dias), categoria do cardápio e situação do pedido. Os mesmos filtros valem para todos os endpoints. As opções saem de `GET /api/dashboard/filtros`, e o filtro fica na URL da tela, então o link pode ser compartilhado. |
| **RF06** | Integração com a API | A tela não recebe números do servidor ao renderizar: tudo vem da API via `fetch`, com as cinco consultas em paralelo. A API lê os pedidos do MongoDB. |
| **RF07** | Atualização e tratamento de dados | Atualização automática a cada 60 s (só com a aba visível) e botão **Atualizar**; uma consulta nova cancela a anterior (`AbortController`). Estados de carregando, vazio e erro; em caso de falha, os números anteriores ficam na tela com o aviso de que podem estar desatualizados. Sessão expirada leva ao login. No servidor: validação dos filtros, valores nulos tratados, divisão por zero evitada, períodos sem venda preenchidos com zero. |

### Regras de negócio dos indicadores

| Regra | Detalhe |
|---|---|
| RN01 | **Pedido cancelado não fatura.** Ele conta em "pedidos" e "cancelamentos", mas fica fora de faturamento, ticket médio, itens, gráficos, categorias e ranking. |
| RN02 | **Faturamento é líquido**, já com o desconto do gerente. |
| RN03 | **O valor vem do pedido**, com o preço "fotografado" na venda. Reajustar o cardápio não altera o histórico. |
| RN04 | **Filtro de categoria** considera só os itens daquela categoria; cada item entra com o seu subtotal menos o mesmo percentual de desconto do pedido. Por arredondamento por item, a soma das categorias pode diferir do total em alguns centavos. |
| RN05 | **Período** é por data de entrada do pedido, inclusive nas duas pontas, no fuso da pizzaria (`America/Sao_Paulo`). Padrão: últimos 30 dias. Máximo: 366 dias. |
| RN06 | **Comparação** é sempre com o período imediatamente anterior, de mesmo tamanho. Sem base (período anterior zerado), a variação vem `null` e a tela mostra "sem base". |
| RN07 | **Ticket médio** = faturamento ÷ pedidos válidos (não cancelados). |
| RN08 | **Em andamento** = pedidos válidos que ainda não chegaram a Finalizado. |
| RN09 | **Produto excluído do cardápio** continua no histórico, na categoria "Sem categoria". |

## 3. Requisitos não funcionais

| ID | Categoria | Requisito |
|---|---|---|
| RNF01 | Segurança | Dashboard só para ADMIN e GERENTE, barrado duas vezes: pela rota (`SecurityConfig`) e no service (`@PreAuthorize`). A API é somente leitura: qualquer método que não seja GET em `/api/dashboard/**` é negado. |
| RNF02 | Segurança | Textos vindos do banco (nome de produto, categoria) são escapados antes de ir para o HTML (`PP.texto`), contra XSS. A biblioteca de gráficos é carregada com SRI (`integrity`). |
| RNF03 | Desempenho | Cada consulta busca no MongoDB só os pedidos do período, usando o índice `idx_entrada_momento` em `entrada.momento`. A tela carrega os cinco blocos em paralelo. |
| RNF04 | Desempenho | Período limitado a 366 dias e ranking a 50 itens, para que nenhuma consulta fique pesada. |
| RNF05 | Atualidade | Respostas com `Cache-Control: no-store`: o navegador nunca mostra números velhos de cache. |
| RNF06 | Confiabilidade | Erros no formato ProblemDetail (RFC 7807) com `codigo` estável, igual ao restante da API. Nenhum stack trace sai para o cliente. |
| RNF07 | Usabilidade | Layout responsivo (filtros em duas colunas e gráfico mais baixo no celular), a mesma identidade visual "Comanda" das outras telas, e cores de status iguais às do quadro de pedidos. |
| RNF08 | Acessibilidade | Cada gráfico tem tabela equivalente; status nunca só por cor (sempre com o nome); `aria-live` no horário da atualização e `aria-busy` durante a carga. |
| RNF09 | Testabilidade | O cálculo dos indicadores não depende de banco nem de Spring (`CalculadoraDashboard`), então as regras são testadas com pedidos montados em memória. A suíte roda sem MongoDB. |
| RNF10 | Manutenibilidade | Mesma arquitetura em camadas do projeto (controller → service → repositório) e mesmas convenções de nome, em português. |

## 4. Decisões de projeto

**Endpoints por pergunta do negócio, não um endpoint único.** Cada bloco da tela
responde a uma pergunta (quanto vendi? quando vendi? onde estão os pedidos? o que mais
sai?). Endpoints separados deixam cada um simples de documentar e testar, e permitem
recarregar só um bloco (trocar o agrupamento do gráfico chama só `/vendas`).

**Cálculo em Java, sobre os pedidos do período, em vez de *aggregation pipeline* do
MongoDB.** As regras (desconto proporcional por categoria, cancelado fora do faturamento,
tempos pela linha do tempo) ficam legíveis e testáveis sem banco. O custo é trazer os
pedidos do período para a memória; para o volume de uma pizzaria (algumas centenas de
pedidos por mês) isso é pequeno, e o índice em `entrada.momento` e o limite de 366 dias
mantêm a consulta barata. Se o volume crescer muito, o mesmo contrato da API pode passar
a ser calculado por agregação no banco sem mudar a tela.

**Comparação com o período anterior no próprio resumo.** Um número sozinho não diz se
está bom; a variação diz. Calcular no servidor evita a tela fazer uma segunda rodada de
consultas.

**Mesmo formato de erro e mesma segurança da parte 1.** O dashboard reaproveita o
`TratadorErrosApi` (ganhou o código `filtro-invalido`, com o nome do `parametro`) e a
lista branca do `SecurityConfig`. Quem já consome a API não precisa aprender nada novo.

**Tela Thymeleaf + JavaScript, sem framework de front.** O projeto já é Thymeleaf; o
dashboard segue o padrão (`Telas.abrir`) e usa o Chart.js só para o gráfico de série,
que é o único que pede eixo e tooltip. As barras de status, categoria e participação são
HTML/CSS. Se o Chart.js não carregar, a tabela de dados abre no lugar do gráfico.

**Dados de demonstração isolados no perfil `demo`.** Para apresentar o painel com 90
dias de histórico sem misturar com dados reais, o `DemoDataInitializer` só roda com
`--spring.profiles.active=demo` e só se o banco não tiver pedidos. Os pedidos passam
pelas mesmas regras da entidade (`abrir`, `avancarPara`, `aplicarDesconto`, `cancelar`).

## 5. Modelo de dados usado

Nenhuma coleção nova. O dashboard lê:

| Coleção | Campos usados |
|---|---|
| `pedidos` | `status`, `entrada.momento`, `itens[].produtoId/descricao/precoUnitario/quantidade`, `subtotal`, `percentualDesconto`, `desconto`, `total`, `linhaDoTempo[].status/carimbo.momento` |
| `produtos` | `_id`, `tipo` (categoria do cardápio) |

Índice novo: `pedidos.idx_entrada_momento` (`{ "entrada.momento": -1 }`).

## 6. Como rodar

```bash
# Normal (dados reais)
./mvnw spring-boot:run

# Com 90 dias de pedidos de demonstração, num banco separado
MONGODB_URI=mongodb://localhost:27017/pediupatrao_demo ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

No Windows: `set MONGODB_URI=...` e `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"`.
Entre com `gerente` ou `admin` (senha `Patrao@2026`) e abra **Dashboard** no menu.
