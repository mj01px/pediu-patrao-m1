# PediuPatrão 🍕

Sistema de pedidos de pizzaria em Java 21, Spring Boot 3.4, Spring Security, MongoDB e Thymeleaf. Esta versão acrescenta perfis de acesso, um fluxo controlado de pedidos (com entrada e saída registradas), descontos e cancelamentos exclusivos do gerente e uma trilha de auditoria.

> Atividade Prática M1 (Parte 2) de Engenharia de Software.

## Executando

| Requisito | Versão |
|---|---|
| JDK | 21 |
| MongoDB Community | 7 ou 8, em `localhost:27017` |
| Maven | não precisa (use o `mvnw`) |

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Abra http://localhost:8080. Na primeira execução são criados três usuários (senha `Patrao@2026`) e um cardápio básico:

| Login | Perfil |
|---|---|
| `admin` | ADMIN |
| `gerente` | GERENTE |
| `atendente` | ATENDENTE |

| Variável de ambiente | Padrão |
|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/pediupatrao` |
| `PEDIUPATRAO_SENHA_INICIAL` | `Patrao@2026` |

Testes (não precisam de MongoDB): `./mvnw test` (57 testes).

## Arquitetura das mudanças

```
Tela / curl ──► SecurityConfig (cadeia /api/** ou cadeia de telas)
                    │  regra por rota e perfil
                    ▼
             Controller ──► Service  (@PreAuthorize: segunda barreira)
                               │
                               ├─► Pedido.avancarPara / cancelar / aplicarDesconto  (regras na entidade)
                               ├─► Repository (MongoDB)
                               └─► publishEvent(RegistroAlteracao)
                                          │
                                          ▼
                                 AuditoriaService (@EventListener) ──► trilha_auditoria
```

* **Regras no domínio:** `Pedido` não tem setters de status nem de valores. Só muda por `abrir`, `avancarPara`, `cancelar` e `aplicarDesconto`, que validam antes de alterar.
* **Auditoria desacoplada:** os services publicam um `RegistroAlteracao`; o `AuditoriaService` escuta e grava. Nenhum service conhece a coleção de auditoria.
* **Diff declarativo:** `ComparadorCampos` lista uma vez quais campos do cliente são auditados e quais são sensíveis (mascarados).
* **Erros em RFC 7807:** a API responde `ProblemDetail` (`type`, `title`, `status`, `detail`, `codigo`).

## Requisitos

### RF01: perfis e autorização

| Operação | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Administrar usuários e perfis | ✅ | | |
| Ver pedidos | ✅ | ✅ | ✅ |
| Registrar pedido e avançar etapas | | ✅ | ✅ |
| Desconto e cancelamento | | ✅ | |
| Consultar clientes | | ✅ | ✅ |
| Cadastrar e alterar clientes | | | ✅ |
| Excluir clientes | | ✅ | |
| Ler auditoria | ✅ | ✅ | |
| Alterar cardápio | ✅ | ✅ | |

* Role lida do banco (`UsuarioDetailsService`); antes todos eram `USER`.
* Duas barreiras: regras por rota no `SecurityConfig` e `@PreAuthorize` com as expressões centralizadas em `RegrasAcesso`.
* A cadeia `/api/**` funciona como lista branca: rota não prevista é negada.
* `/api/usuarios` deixou de ser público. Ninguém altera o próprio perfil nem remove a si mesmo, e sempre sobra um ADMIN.
* CSRF ativo nas telas (token em formulários e no cabeçalho dos `fetch`). Chamadas com HTTP Basic dispensam o token porque não usam cookie.

### RF02: pedido completo

Itens com preço copiado do cardápio, quantidades, subtotal, percentual e valor de desconto, total, responsável e momento de entrada. A API recebe só `clienteId`, `produtoId` e `quantidade`.

### RF03: entrada, saída e transições

```
RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO
                                  └► RETIRADO ─────────┘
```

O mapa de transições fica em `StatusPedido`. Entrada e saída são "carimbadas" (`Carimbo`: quem e quando) e cada etapa entra na linha do tempo do pedido. Pedido cancelado não aceita mais nada.

### RF04: desconto e cancelamento (só GERENTE)

* `PUT /api/pedidos/{id}/desconto` com `{"percentual": 10}`: de 0,01% a 20%, só antes da saída.
* `POST /api/pedidos/{id}/cancelamento` com `{"motivo": "..."}`: motivo com 10+ caracteres, só antes da saída, valores preservados.

### RF05: trilha de auditoria

Coleção `trilha_auditoria`: momento, usuário, perfil, tipo de evento, entidade e id, mudanças (de → para) e justificativa. Cobre inclusão de pedido, mudança de etapa, desconto, cancelamento, inclusão/alteração/exclusão de cliente e de usuário.

* Leitura só para ADMIN e GERENTE (`/auditoria` e `GET /api/auditoria`).
* Imutável: entidade sem setters, repositório estende `Repository` só com `save` e consultas, e não há endpoint de escrita (405).
* Dados pessoais: telefone mascarado, senha nunca registrada, exclusão de cliente guarda só o nome, log só com metadados.

## Formato de erro

```json
{
  "type": "urn:pediupatrao:regra-de-negocio",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Não é possível ir de Em preparação para Finalizado. Próximas etapas: [PRONTO].",
  "codigo": "regra-de-negocio"
}
```

| Status | `codigo` |
|---|---|
| 400 | `dados-invalidos` (com lista `erros`), `formato-invalido` |
| 401 | `nao-autenticado` |
| 403 | `acesso-negado`, `csrf-invalido` |
| 404 | `nao-encontrado` |
| 409 | `alterado-por-outro` |
| 422 | `regra-de-negocio` |

## Vídeo

_(link do vídeo aqui)_
