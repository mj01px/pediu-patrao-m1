<div align="center">

<br/>

<a href="https://git.io/typing-svg">
  <img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&weight=600&size=30&pause=1000&color=D7341C&center=true&vCenter=true&width=560&lines=PediuPatr%C3%A3o;Anota.+Prepara.+Entrega.+Audita." alt="Typing SVG" />
</a>

<br/>

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring_Security-6-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/>
  <img src="https://img.shields.io/badge/Thymeleaf-3-005F0F?style=flat-square&logo=thymeleaf&logoColor=white"/>
  <img src="https://img.shields.io/badge/MongoDB-7+-47A248?style=flat-square&logo=mongodb&logoColor=white"/>
  <img src="https://img.shields.io/badge/JUnit-5-25A162?style=flat-square&logo=junit5&logoColor=white"/>
</p>

</div>

<br/>

---

## `~/sobre`

```ts
const pediuPatrao = {
  tipo:        "Sistema de pedidos para pizzaria (Atividade Prática M1, Engenharia de Software)",
  backend:     ["Java 21", "Spring Boot 3.4", "Spring Security 6", "Spring Data MongoDB", "JUnit 5 + Mockito"],
  telas:       ["Thymeleaf", "CSS próprio, sem framework", "Ícones em sprite SVG", "<dialog> nativo"],
  perfis:      ["ADMIN", "GERENTE", "ATENDENTE"],
  recursos:    ["Quadro de comandas", "Fluxo de status controlado", "Desconto e cancelamento do gerente", "Trilha de auditoria"],
  autor:       "Mauro Junior · github.com/mj01px",
} as const;
```

O **PediuPatrão** organiza o balcão de uma pizzaria: o atendente cadastra o cliente e
anota o pedido, a cozinha acompanha cada comanda por um quadro de etapas e o gerente é o
único que pode dar desconto ou cancelar. Cada mudança relevante vira um registro na
**trilha de auditoria**, com quem fez, quando, o valor anterior e o novo.

O ponto de partida foi o projeto base entregue pelo professor. Sobre ele vieram três
perfis de acesso aplicados no servidor (e não só escondendo botões), um pedido que só
muda de estado por regras da própria entidade, o cálculo do total feito no servidor e a
auditoria persistida no MongoDB. As telas foram refeitas do zero com uma identidade
própria, a **Comanda**.

```
PediuPatrao/
├── src/main/java/.../config       # SecurityConfig, carga inicial, relógio
├── src/main/java/.../seguranca    # regras de acesso, respostas 401/403, encerramento de sessões
├── src/main/java/.../service      # regras de negócio + @PreAuthorize
├── src/main/java/.../entity       # Pedido, StatusPedido, EventoAuditoria...
├── src/main/resources/templates   # telas Thymeleaf      →  http://localhost:8080
└── src/test/java                  # 64 testes, sem MongoDB
```

---

## `~/recursos`

<table>
  <tr>
    <td valign="top" width="50%">
      <b>🍕 Balcão e cozinha</b><br/><br/>
      <ul>
        <li>Início com a contagem de pedidos por etapa e os últimos pedidos</li>
        <li>Quadro de comandas por etapa, com busca por cliente ou código</li>
        <li>Novo pedido com cardápio agrupado por tipo e resumo ao vivo</li>
        <li>Detalhe com itens, desconto, total e linha do tempo das etapas</li>
        <li>Código curto em cada comanda (<code>#6A1F</code>) para falar com a cozinha</li>
        <li>Cadastro de clientes com busca de CEP (ViaCEP)</li>
      </ul>
      <br/>
      <b>💸 Gerente</b><br/><br/>
      <ul>
        <li>Desconto percentual de até 20%, com prévia do novo total</li>
        <li>Cancelamento com justificativa obrigatória</li>
        <li>Os dois só antes de o pedido sair</li>
      </ul>
    </td>
    <td valign="top" width="50%">
      <b>🔒 Acesso</b><br/><br/>
      <ul>
        <li>Três perfis lidos do banco: ADMIN, GERENTE e ATENDENTE</li>
        <li>Duas barreiras: regra por rota e <code>@PreAuthorize</code> nos services</li>
        <li>API em lista branca: rota não prevista é negada</li>
        <li>Ninguém muda o próprio perfil nem se remove; sempre sobra um ADMIN</li>
        <li>Sessão derrubada quando o perfil, o login ou a senha mudam</li>
        <li>CSRF nas telas e nos <code>fetch</code></li>
      </ul>
      <br/>
      <b>🧾 Auditoria</b><br/><br/>
      <ul>
        <li>Pedidos, etapas, descontos, cancelamentos, clientes e usuários</li>
        <li>Login e logout de cada funcionário, com o horário</li>
        <li>Usuário, perfil, data/hora, entidade, campos (antes → depois) e justificativa</li>
        <li>Somente leitura, para ADMIN e GERENTE</li>
        <li>Telefone mascarado e senha nunca registrada</li>
      </ul>
    </td>
  </tr>
</table>

---

## `~/começando`

Precisa do **JDK 21** e de um **MongoDB** rodando em `localhost:27017`. O Maven não
precisa estar instalado, o `mvnw` baixa sozinho.

```bash
./mvnw spring-boot:run                 # Windows: mvnw.cmd spring-boot:run  →  http://localhost:8080
```

Na primeira execução o sistema cria um cardápio básico e três usuários, todos com a
senha `Patrao@2026`:

| Login | Perfil |
|---|---|
| `admin` | ADMIN |
| `gerente` | GERENTE |
| `atendente` | ATENDENTE |

> A porta 8080 ocupada (a Steam, por exemplo, fica nela)? Suba em outra:
> `mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"`

---

## `~/ambiente`

Nenhuma variável é obrigatória para rodar localmente.

```env
# Conexão do MongoDB (padrão: o MongoDB local)
MONGODB_URI=mongodb://localhost:27017/pediupatrao

# Senha dos três usuários criados na primeira execução
PEDIUPATRAO_SENHA_INICIAL=Patrao@2026
```

O fuso usado para carimbar entrada, saída e auditoria é fixo em `America/Sao_Paulo`
(`pediupatrao.fuso-horario`), então o horário não depende do servidor.

---

## `~/requisitos`

<details>
<summary><b>RF01 · Perfis e permissões</b></summary>
<br/>

A role vem do perfil gravado no banco (`UsuarioDetailsService`); no projeto base todo
mundo recebia `USER`. Usuário sem perfil não consegue entrar.

| Operação | ADMIN | GERENTE | ATENDENTE |
|---|:-:|:-:|:-:|
| Administrar usuários e perfis | ✅ | | |
| Consultar pedidos | ✅ | ✅ | ✅ |
| Registrar pedido e avançar etapas | | ✅ | ✅ |
| Desconto e cancelamento | | ✅ | |
| Consultar clientes | | ✅ | ✅ |
| Cadastrar e alterar clientes | | | ✅ |
| Excluir clientes | | ✅ | |
| Ler a auditoria | ✅ | ✅ | |
| Alterar o cardápio | ✅ | ✅ | |

Toda regra existe duas vezes. O `SecurityConfig` barra pela rota e pelo método HTTP, e
os services repetem a regra com `@PreAuthorize`, usando as expressões centralizadas em
`RegrasAcesso`. Assim uma chamada que escape da rota ainda bate no service. A cadeia
`/api/**` termina em `denyAll()`, então uma rota nova nasce fechada.

Para impedir autoelevação, o `UsuarioService` recusa que alguém altere o próprio perfil
ou remova a si mesmo, e não deixa o último ADMIN perder o perfil.

As roles ficam guardadas na sessão no momento do login. Por isso, quando o ADMIN muda
o perfil, o login ou a senha de alguém, ou remove o usuário, o `EncerradorDeSessoes`
derruba as sessões abertas dessa pessoa pelo `SessionRegistry`. Nas telas ela volta ao
login com um aviso; na API recebe `401 sessao-encerrada`. O painel legado `/admin`
também passou a exigir ADMIN.

</details>

<details>
<summary><b>RF02 · Pedido completo, com total do servidor</b></summary>
<br/>

O pedido guarda os itens (produto, nome e preço copiados do cardápio no momento da
venda, quantidade e subtotal), o percentual e o valor do desconto, o total, quem
registrou e quando entrou.

A API recebe só `clienteId` e a lista de `produtoId` + `quantidade`. Preço, subtotal e
total são sempre calculados no servidor, e produto pausado ou quantidade inválida é
recusado. O `Pedido` não tem setters de status nem de valores.

</details>

<details>
<summary><b>RF03 · Entrada, saída e transições</b></summary>
<br/>

```
RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA → FINALIZADO
                                  └► RETIRADO ──────────┘
```

O mapa de transições mora em `StatusPedido`, e o pedido só muda por `avancarPara`, que
valida antes de alterar. Pular etapa (finalizar um pedido em preparação, por exemplo)
responde `422` com as próximas etapas possíveis. A entrada e a saída são carimbadas com
quem e quando (`Carimbo`), e cada etapa entra na linha do tempo do pedido. Cancelado é
estado final: nada mais muda depois dele.

Duas pessoas mexendo no mesmo pedido ao mesmo tempo não se atropelam: o `@Version` do
pedido faz a segunda gravação responder `409 alterado-por-outro`.

</details>

<details>
<summary><b>RF04 · Desconto e cancelamento, só do GERENTE</b></summary>
<br/>

| Operação | Regra |
|---|---|
| `PUT /api/pedidos/{id}/desconto` · `{"percentual": 10}` | De 0,01% a 20%, só antes da saída; um novo desconto substitui o anterior |
| `POST /api/pedidos/{id}/cancelamento` · `{"motivo": "..."}` | Motivo com pelo menos 10 caracteres, só antes da saída, valores preservados |

Nem o ADMIN consegue: a rota exige GERENTE e o service confere de novo. As duas
operações vão para a auditoria com o antes e o depois.

</details>

<details>
<summary><b>RF05 · Trilha de auditoria</b></summary>
<br/>

Os services não conhecem a auditoria. Eles publicam um `RegistroAlteracao` e o
`AuditoriaService` escuta o evento e grava na coleção `trilha_auditoria`.

```
Controller → Service → regra na entidade → MongoDB
                  └──► publishEvent(RegistroAlteracao) → AuditoriaService → trilha_auditoria
```

| Evento | Quando |
|---|---|
| Inclusão de pedido · mudança de status | Ao registrar e a cada etapa |
| Desconto aplicado · cancelamento | Com percentual ou justificativa |
| Inclusão · alteração · exclusão de cliente | Com cada campo alterado, antes e depois |
| Inclusão · alteração · exclusão de usuário | Perfil e login; a senha aparece só como "redefinida" |
| Login · logout no sistema | Quem entrou e saiu pela tela, com perfil e horário (aba **Logins**) |

O login e o logout pela tela também entram, pelo `RegistroDeAcessos`, que escuta os
eventos do Spring Security. Só o login e o perfil são gravados, nunca a senha, e as
chamadas à API com HTTP Basic ficam de fora para não lotar a trilha.

O `ComparadorCampos` declara uma vez quais campos do cliente são auditados e quais são
sensíveis. O telefone aparece só com os 4 últimos dígitos, e a exclusão de cliente
guarda só o nome.

A trilha é imutável pelas interfaces: a entidade não tem setters, o repositório estende
`Repository` só com `save` e consultas, e a API só tem `GET`. A consulta, em
`/auditoria` e `GET /api/auditoria?entidade=&registroId=`, é exclusiva de ADMIN e
GERENTE.

</details>

---

## `~/api`

<details>
<summary><b>Rotas e formato de erro</b></summary>
<br/>

As rotas aceitam a sessão das telas (com CSRF) ou HTTP Basic, para testar com `curl`
ou Postman. Chamadas com Basic dispensam o token CSRF porque não usam cookie.

| Rota | O que faz |
|---|---|
| `GET` · `POST /api/pedidos` | Lista e registra pedidos |
| `GET /api/pedidos/{id}` | Um pedido |
| `PUT /api/pedidos/{id}/status` | Avança a etapa |
| `PUT /api/pedidos/{id}/desconto` · `POST .../cancelamento` | Operações do gerente |
| `GET` · `POST /api/clientes` · `PUT` · `DELETE /api/clientes/{id}` | Clientes |
| `GET` · `POST /api/produtos` · `PUT` · `DELETE /api/produtos/{id}` | Cardápio |
| `GET` · `POST /api/usuarios` · `GET` · `PUT` · `DELETE /api/usuarios/{id}` | Usuários (ADMIN) |
| `GET /api/auditoria` | Trilha, com filtros `entidade` e `registroId` |

Todo erro sai em `ProblemDetail` (RFC 7807), com um `codigo` estável:

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
| 400 | `dados-invalidos` (com a lista `erros`), `formato-invalido` |
| 401 | `nao-autenticado`, `sessao-encerrada` |
| 403 | `acesso-negado`, `csrf-invalido` |
| 404 | `nao-encontrado` |
| 409 | `alterado-por-outro` |
| 422 | `regra-de-negocio` |

Para ver o bloqueio no backend sem passar pela tela:

```bash
# Atendente tentando dar desconto → 403 acesso-negado
curl -u atendente:Patrao@2026 -X PUT http://localhost:8080/api/pedidos/<id>/desconto \
     -H "Content-Type: application/json" -d '{"percentual": 10}'
```

</details>

---

## `~/telas`

As telas usam Thymeleaf com um CSS próprio (`static/css/style.css`), sem Bootstrap. A
identidade **Comanda** usa Bricolage Grotesque nos títulos, Figtree no texto e JetBrains
Mono em códigos e valores, sobre um fundo pedra com o vermelho tomate para as ações. As
etapas do pedido, os perfis e os tipos de evento da auditoria têm cada um a sua cor. Os
ícones saem de um único sprite (`static/images/icones.svg`), e os modais usam o
`<dialog>` nativo. No celular o menu vira uma gaveta lateral.

Os botões aparecem conforme o perfil, mas são só conveniência: quem decide é sempre o
servidor.

---

## `~/testes`

```bash
./mvnw test                            # 64 testes, não precisam de MongoDB
```

| Suíte | O que prova |
|---|---|
| `AcessoApiTest` | Cada perfil batendo nas rotas da API, inclusive as proibidas (401/403) |
| `AcessoTelasTest` | `/admin` só abre para ADMIN |
| `SegurancaMetodosTest` | O `@PreAuthorize` barra o perfil errado direto no service |
| `StatusPedidoTest` · `PedidoTest` | Transições válidas e inválidas, limite de desconto, bloqueio após cancelar |
| `PedidoServiceTest` | Preço vindo do cardápio, produto fora de venda recusado, desconto inválido sem gravar nem auditar |
| `ComparadorCamposTest` | Só o que mudou entra no registro, com o antes e o depois, e o telefone vai mascarado |
| `EncerradorDeSessoesTest` | Só as sessões do usuário alterado caem |
| `RegistroDeAcessosTest` | Login e logout gravados com usuário, perfil e horário |

---

## `~/vídeo`

_(link do vídeo aqui)_

---

## `~/stack`

<div align="center">

| Camada | Tecnologias |
|-------|-------------|
| **Backend** | ![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4-6DB33F?style=flat-square&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white) ![Lombok](https://img.shields.io/badge/Lombok-BC4521?style=flat-square&logoColor=white) |
| **Telas** | ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?style=flat-square&logo=thymeleaf&logoColor=white) ![CSS](https://img.shields.io/badge/CSS_próprio-663399?style=flat-square&logo=css&logoColor=white) ![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black) |
| **Banco** | ![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square&logo=mongodb&logoColor=white) |
| **Testes** | ![JUnit](https://img.shields.io/badge/JUnit_5-25A162?style=flat-square&logo=junit5&logoColor=white) ![Mockito](https://img.shields.io/badge/Mockito-78A641?style=flat-square&logoColor=white) |

</div>

---

<div align="center">
  <br/>
  <sub>
    Feito por <a href="https://github.com/mj01px"><strong>Mauro Junior</strong></a>
    &nbsp;·&nbsp;
    <a href="https://www.linkedin.com/in/mauroapjunior/">LinkedIn</a>
  </sub>
  <br/><br/>
</div>
