package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Carimbo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Historico de demonstracao para o dashboard: clientes, mais itens no cardapio e cerca de
 * 90 dias de pedidos, com picos no jantar e no fim de semana, descontos e cancelamentos.
 *
 * So roda com o perfil "demo" (--spring.profiles.active=demo) e so se ainda nao houver
 * pedidos, para nunca misturar dados ficticios com dados reais.
 * Os pedidos passam pelas mesmas regras da entidade (abrir, avancarPara, aplicarDesconto, cancelar).
 */
@Component
@Profile("demo")
@Order(10) // depois do DataInitializer, que cria o cardapio basico
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final int DIAS = 90;

    private static final String[][] CARDAPIO_EXTRA = {
        {"Pizza Margherita", "Pizza", "46.00"},
        {"Pizza Portuguesa", "Pizza", "54.00"},
        {"Pizza Pepperoni", "Pizza", "59.90"},
        {"Esfiha de Queijo", "Esfiha", "7.50"},
        {"Esfiha de Frango", "Esfiha", "7.50"},
        {"X-Burguer", "Lanche", "28.00"},
        {"X-Salada", "Lanche", "31.00"},
        {"Coca-Cola 2L", "Bebida", "14.00"},
        {"Suco Natural 500ml", "Bebida", "10.00"},
        {"Pizza Doce de Chocolate", "Sobremesa", "44.00"},
    };

    private static final String[] NOMES = {
        "Ana Souza", "Bruno Lima", "Carla Mendes", "Diego Rocha", "Eduarda Alves", "Felipe Costa",
        "Gabriela Nunes", "Henrique Dias", "Isabela Martins", "João Pereira", "Karina Gomes",
        "Lucas Ribeiro", "Mariana Torres", "Natália Freitas", "Otávio Barros", "Paula Cardoso",
    };

    private static final String[] MOTIVOS = {
        "Cliente desistiu do pedido por telefone",
        "Endereço fora da área de entrega",
        "Pedido duplicado pelo atendente",
        "Cliente não atendeu na confirmação",
    };

    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final ClienteRepository clientes;
    private final Clock relogio;
    private final Random sorteio = new Random(2026);

    public DemoDataInitializer(PedidoRepository pedidos, ProdutoRepository produtos,
            ClienteRepository clientes, Clock relogio) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.clientes = clientes;
        this.relogio = relogio;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (pedidos.count() > 0) {
            log.info("Perfil demo: já existem pedidos, a carga de demonstração foi ignorada.");
            return;
        }
        completarCardapio();
        List<Produto> cardapio = produtos.findAll().stream().filter(Produto::disponivelParaVenda).toList();
        List<Cliente> carteira = criarClientes();

        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDate hoje = agora.toLocalDate();
        List<Pedido> lote = new ArrayList<>();
        for (int d = DIAS - 1; d >= 0; d--) {
            LocalDate dia = hoje.minusDays(d);
            int fimDeSemana = dia.getDayOfWeek().getValue() >= 5 ? 6 : 0; // sexta, sabado e domingo vendem mais
            int crescimento = (DIAS - d) / 20; // movimento crescendo ao longo do trimestre
            int quantidade = 6 + fimDeSemana + crescimento + sorteio.nextInt(5);
            for (int i = 0; i < quantidade; i++) {
                LocalDateTime entrada = dia.atTime(horaDoPedido(), sorteio.nextInt(60));
                if (entrada.isAfter(agora)) {
                    continue;
                }
                lote.add(montarPedido(entrada, agora, cardapio, carteira));
            }
        }
        pedidos.saveAll(lote);
        log.info("Perfil demo: {} pedidos de demonstração criados em {} dias.", lote.size(), DIAS);
    }

    private Pedido montarPedido(LocalDateTime entrada, LocalDateTime agora, List<Produto> cardapio,
            List<Cliente> carteira) {
        Cliente cliente = carteira.get(sorteio.nextInt(carteira.size()));
        List<ItemPedido> itens = new ArrayList<>();
        int linhas = 1 + sorteio.nextInt(3);
        for (int i = 0; i < linhas; i++) {
            Produto produto = escolherProduto(cardapio);
            itens.add(new ItemPedido(produto, produto.getTipo().equals("Esfiha") ? 2 + sorteio.nextInt(6) : 1 + sorteio.nextInt(2)));
        }
        Pedido pedido = Pedido.abrir(cliente, itens, null, new Carimbo("atendente", entrada));

        int sorte = sorteio.nextInt(100);
        if (sorte < 12) {
            pedido.aplicarDesconto(new BigDecimal(5 + 5 * sorteio.nextInt(3)), new Carimbo("gerente", entrada.plusMinutes(2)));
        }
        if (sorte >= 94) {
            pedido.cancelar(MOTIVOS[sorteio.nextInt(MOTIVOS.length)], new Carimbo("gerente", entrada.plusMinutes(5)));
            return pedido;
        }

        // Cada etapa so acontece se o horario dela ja passou: pedidos de agora ficam em andamento
        LocalDateTime preparo = entrada.plusMinutes(3 + sorteio.nextInt(6));
        LocalDateTime pronto = preparo.plusMinutes(18 + sorteio.nextInt(15));
        boolean entrega = sorteio.nextInt(100) < 65;
        LocalDateTime saida = pronto.plusMinutes(entrega ? 4 + sorteio.nextInt(10) : 5 + sorteio.nextInt(20));
        LocalDateTime fim = saida.plusMinutes(entrega ? 15 + sorteio.nextInt(20) : 1);

        avancar(pedido, StatusPedido.EM_PREPARACAO, preparo, agora);
        avancar(pedido, StatusPedido.PRONTO, pronto, agora);
        avancar(pedido, entrega ? StatusPedido.SAIU_PARA_ENTREGA : StatusPedido.RETIRADO, saida, agora);
        avancar(pedido, StatusPedido.FINALIZADO, fim, agora);
        return pedido;
    }

    private void avancar(Pedido pedido, StatusPedido destino, LocalDateTime quando, LocalDateTime agora) {
        if (!quando.isAfter(agora) && pedido.getStatus().permite(destino)) {
            pedido.avancarPara(destino, new Carimbo("atendente", quando));
        }
    }

    // Pizzas pesam mais que os outros itens, como numa pizzaria de verdade
    private Produto escolherProduto(List<Produto> cardapio) {
        Produto produto = cardapio.get(sorteio.nextInt(cardapio.size()));
        if (!"Pizza".equals(produto.getTipo()) && sorteio.nextInt(100) < 35) {
            List<Produto> pizzas = cardapio.stream().filter(p -> "Pizza".equals(p.getTipo())).toList();
            return pizzas.isEmpty() ? produto : pizzas.get(sorteio.nextInt(pizzas.size()));
        }
        return produto;
    }

    // Almoco fraco (11h-14h), jantar forte (18h-23h)
    private int horaDoPedido() {
        int faixa = sorteio.nextInt(100);
        if (faixa < 15) {
            return 11 + sorteio.nextInt(4);
        }
        if (faixa < 25) {
            return 15 + sorteio.nextInt(3);
        }
        int[] jantar = {18, 19, 19, 20, 20, 20, 21, 21, 22, 23};
        return jantar[sorteio.nextInt(jantar.length)];
    }

    private void completarCardapio() {
        List<String> existentes = produtos.findAll().stream().map(Produto::getNome).toList();
        for (String[] linha : CARDAPIO_EXTRA) {
            if (!existentes.contains(linha[0])) {
                Produto produto = new Produto();
                produto.setNome(linha[0]);
                produto.setTipo(linha[1]);
                produto.setPreco(Double.valueOf(linha[2]));
                produto.setAtivo(true);
                produtos.save(produto);
            }
        }
    }

    private List<Cliente> criarClientes() {
        List<Cliente> carteira = new ArrayList<>(clientes.findAll());
        if (carteira.size() >= 10) {
            return carteira;
        }
        for (int i = 0; i < NOMES.length; i++) {
            Cliente cliente = new Cliente();
            cliente.setNome(NOMES[i]);
            cliente.setTelefone(String.format("(11) 9%04d-%04d", 1000 + i * 37, 2000 + i * 53));
            cliente.setCidade("Mogi das Cruzes");
            cliente.setEstado("SP");
            carteira.add(clientes.save(cliente));
        }
        return carteira;
    }
}
