package com.umc.pediupatrao.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Categoria;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Etapa;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Indicadores;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.PontoVenda;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.ProdutoVendido;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Tempos;
import com.umc.pediupatrao.entity.Carimbo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regras dos indicadores do dashboard, sem Spring e sem MongoDB.
 *
 * Cenario (periodo 01/10 a 03/10/2026):
 *  P1 01/10 19h  2 Calabresa (99,80) + 1 Guarana (13,00) = 112,80   FINALIZADO
 *  P2 01/10 20h  1 Calabresa (49,90) com 10% de desconto = 44,91     EM_PREPARACAO
 *  P3 03/10 12h  4 Esfiha (28,00)                         = 28,00     CANCELADO
 */
class CalculadoraDashboardTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 10, 3);
    private static final Map<String, String> TIPOS = Map.of("cal", "Pizza", "gua", "Bebida", "esf", "Esfiha");

    private Produto calabresa;
    private Produto guarana;
    private Produto esfiha;
    private List<Pedido> pedidos;

    @BeforeEach
    void montarCenario() {
        calabresa = produto("cal", "Pizza Calabresa", 49.90);
        guarana = produto("gua", "Guaraná 2L", 13.00);
        esfiha = produto("esf", "Esfiha de Carne", 7.00);

        LocalDateTime d1 = LocalDateTime.of(2026, 10, 1, 19, 0);
        Pedido p1 = abrir(d1, new ItemPedido(calabresa, 2), new ItemPedido(guarana, 1));
        p1.avancarPara(StatusPedido.EM_PREPARACAO, em(d1.plusMinutes(5)));
        p1.avancarPara(StatusPedido.PRONTO, em(d1.plusMinutes(25)));
        p1.avancarPara(StatusPedido.SAIU_PARA_ENTREGA, em(d1.plusMinutes(30)));
        p1.avancarPara(StatusPedido.FINALIZADO, em(d1.plusMinutes(50)));

        LocalDateTime d2 = LocalDateTime.of(2026, 10, 1, 20, 0);
        Pedido p2 = abrir(d2, new ItemPedido(calabresa, 1));
        p2.aplicarDesconto(BigDecimal.TEN, em(d2.plusMinutes(1)));
        p2.avancarPara(StatusPedido.EM_PREPARACAO, em(d2.plusMinutes(3)));

        LocalDateTime d3 = LocalDateTime.of(2026, 10, 3, 12, 0);
        Pedido p3 = abrir(d3, new ItemPedido(esfiha, 4));
        p3.cancelar("Cliente desistiu do pedido", em(d3.plusMinutes(2)));

        pedidos = List.of(p1, p2, p3);
    }

    @Test
    void indicadoresIgnoramCanceladoNoFaturamentoMasContamNaTaxa() {
        Indicadores i = calcular(null, null).indicadores();

        assertThat(i.faturamento()).isEqualByComparingTo("157.71"); // 112,80 + 44,91
        assertThat(i.pedidos()).isEqualTo(3);
        assertThat(i.pedidosValidos()).isEqualTo(2);
        assertThat(i.ticketMedio()).isEqualByComparingTo("78.86");
        assertThat(i.itensVendidos()).isEqualTo(4); // 2 + 1 + 1; as 4 esfihas canceladas nao contam
        assertThat(i.cancelados()).isEqualTo(1);
        assertThat(i.taxaCancelamento()).isEqualByComparingTo("33.3");
        assertThat(i.descontoConcedido()).isEqualByComparingTo("4.99");
        assertThat(i.pedidosComDesconto()).isEqualTo(1);
        assertThat(i.emAndamento()).isEqualTo(1);
    }

    @Test
    void filtroDeCategoriaSomaSoOsItensDelaComODescontoProporcional() {
        Indicadores i = calcular("pizza", null).indicadores(); // sem diferenciar maiusculas

        // P1: 99,80 de pizza; P2: 49,90 - 10% = 44,91. O guarana fica de fora.
        assertThat(i.faturamento()).isEqualByComparingTo("144.71");
        assertThat(i.pedidos()).isEqualTo(2); // P3 nao tem pizza
        assertThat(i.itensVendidos()).isEqualTo(3);
    }

    @Test
    void filtroDeStatusRestringeOsPedidos() {
        Indicadores i = calcular(null, StatusPedido.FINALIZADO).indicadores();

        assertThat(i.pedidos()).isEqualTo(1);
        assertThat(i.faturamento()).isEqualByComparingTo("112.80");
        assertThat(i.cancelados()).isZero();
    }

    @Test
    void periodoSemPedidosDevolveZerosSemDividirPorZero() {
        Indicadores i = new CalculadoraDashboard(List.of(), TIPOS, filtro(null, null)).indicadores();

        assertThat(i.faturamento()).isEqualByComparingTo("0");
        assertThat(i.ticketMedio()).isEqualByComparingTo("0");
        assertThat(i.taxaCancelamento()).isEqualByComparingTo("0");
    }

    @Test
    void serieDiariaPreencheOsDiasSemVenda() {
        List<PontoVenda> serie = calcular(null, null).serie(Agrupamento.DIA);

        assertThat(serie).extracting(PontoVenda::chave).containsExactly("2026-10-01", "2026-10-02", "2026-10-03");
        assertThat(serie.get(0).pedidos()).isEqualTo(2);
        assertThat(serie.get(0).faturamento()).isEqualByComparingTo("157.71");
        assertThat(serie.get(1).pedidos()).isZero();
        assertThat(serie.get(2).pedidos()).isZero(); // o unico pedido do dia 3 foi cancelado
    }

    @Test
    void seriePorHoraTemAs24FaixasDoDia() {
        List<PontoVenda> serie = calcular(null, null).serie(Agrupamento.HORA);

        assertThat(serie).hasSize(24);
        assertThat(serie.get(19).faturamento()).isEqualByComparingTo("112.80");
        assertThat(serie.get(20).faturamento()).isEqualByComparingTo("44.91");
        assertThat(serie.get(12).pedidos()).isZero();
    }

    @Test
    void seriePorSemanaComecaNaSegunda() {
        List<PontoVenda> serie = calcular(null, null).serie(Agrupamento.SEMANA);

        // 01/10/2026 e quinta-feira: a semana comeca na segunda 28/09
        assertThat(serie).extracting(PontoVenda::chave).containsExactly("2026-09-28");
        assertThat(serie.get(0).pedidos()).isEqualTo(2);
    }

    @Test
    void categoriasOrdenadasPorFaturamentoComParticipacao() {
        List<Categoria> categorias = calcular(null, null).categorias();

        assertThat(categorias).extracting(Categoria::tipo).containsExactly("Pizza", "Bebida");
        assertThat(categorias.get(0).faturamento()).isEqualByComparingTo("144.71");
        assertThat(categorias.get(0).participacao()).isEqualByComparingTo("91.8");
    }

    @Test
    void etapasTrazemTodosOsStatusNaOrdemDoFluxo() {
        List<Etapa> etapas = calcular(null, null).etapas();

        assertThat(etapas).extracting(Etapa::status).containsExactly(StatusPedido.values());
        assertThat(etapas).filteredOn(e -> e.quantidade() > 0).extracting(Etapa::status)
                .containsExactly(StatusPedido.EM_PREPARACAO, StatusPedido.FINALIZADO, StatusPedido.CANCELADO);
    }

    @Test
    void temposMediosSaemDaLinhaDoTempo() {
        Tempos tempos = calcular(null, null).tempos();

        assertThat(tempos.preparoMedioMin()).isEqualTo(25);
        assertThat(tempos.saidaMediaMin()).isEqualTo(30);
        assertThat(tempos.conclusaoMediaMin()).isEqualTo(50);
    }

    @Test
    void rankingPorQuantidadeEPorFaturamento() {
        CalculadoraDashboard calculo = calcular(null, null);

        List<ProdutoVendido> porQuantidade = calculo.ranking(10, OrdemRanking.QUANTIDADE);
        assertThat(porQuantidade).extracting(ProdutoVendido::nome).containsExactly("Pizza Calabresa", "Guaraná 2L");
        assertThat(porQuantidade.get(0).quantidade()).isEqualTo(3);
        assertThat(porQuantidade.get(0).posicao()).isEqualTo(1);
        assertThat(porQuantidade.get(0).tipo()).isEqualTo("Pizza");

        assertThat(calculo.ranking(1, OrdemRanking.FATURAMENTO)).singleElement()
                .satisfies(p -> assertThat(p.faturamento()).isEqualByComparingTo("144.71"));
    }

    @Test
    void produtoRemovidoDoCardapioApareceSemCategoria() {
        CalculadoraDashboard calculo = new CalculadoraDashboard(pedidos, Map.of(), filtro(null, null));

        assertThat(calculo.categorias()).extracting(Categoria::tipo).containsExactly(CalculadoraDashboard.SEM_CATEGORIA);
    }

    @Test
    void variacaoSemBaseENula() {
        assertThat(CalculadoraDashboard.variacao(new BigDecimal("150"), new BigDecimal("100"))).isEqualByComparingTo("50.0");
        assertThat(CalculadoraDashboard.variacao(new BigDecimal("50"), new BigDecimal("100"))).isEqualByComparingTo("-50.0");
        assertThat(CalculadoraDashboard.variacao(BigDecimal.TEN, BigDecimal.ZERO)).isNull();
    }

    private CalculadoraDashboard calcular(String tipo, StatusPedido status) {
        return new CalculadoraDashboard(pedidos, TIPOS, filtro(tipo, status));
    }

    private static FiltroDashboard filtro(String tipo, StatusPedido status) {
        return FiltroDashboard.de(INICIO, FIM, tipo, status, FIM);
    }

    private static Pedido abrir(LocalDateTime quando, ItemPedido... itens) {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        return Pedido.abrir(cliente, List.of(itens), null, em(quando));
    }

    private static Carimbo em(LocalDateTime quando) {
        return new Carimbo("teste", quando);
    }

    private static Produto produto(String id, String nome, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
