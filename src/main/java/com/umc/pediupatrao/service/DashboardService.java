package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Comparacao;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.FiltroAplicado;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Indicadores;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Opcao;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.RankingProdutos;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Resumo;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.SerieVendas;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.SituacaoPedidos;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.VendasPorCategoria;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.erro.FiltroInvalidoException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.seguranca.RegrasAcesso;
import com.umc.pediupatrao.service.dashboard.Agrupamento;
import com.umc.pediupatrao.service.dashboard.CalculadoraDashboard;
import com.umc.pediupatrao.service.dashboard.FiltroDashboard;
import com.umc.pediupatrao.service.dashboard.OrdemRanking;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Dados do Dashboard Administrativo (RF01 a RF05). Busca no MongoDB so os pedidos do periodo
 * (indice em entrada.momento) e entrega o calculo para a CalculadoraDashboard.
 *
 * Leitura restrita a quem administra o estabelecimento: ADMIN e GERENTE.
 */
@Service
public class DashboardService {

    public static final int LIMITE_RANKING_PADRAO = 10;
    public static final int LIMITE_RANKING_MAXIMO = 50;

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final Clock relogio;

    public DashboardService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository, Clock relogio) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.relogio = relogio;
    }

    /** Monta o filtro com os padroes (ultimos 30 dias) e valida o periodo. */
    public FiltroDashboard filtro(LocalDate inicio, LocalDate fim, String tipo, StatusPedido status) {
        return FiltroDashboard.de(inicio, fim, tipo, status, LocalDate.now(relogio));
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public OpcoesFiltro opcoes() {
        FiltroDashboard padrao = filtro(null, null, null, null);
        List<String> tipos = produtoRepository.findAll().stream()
                .map(Produto::getTipo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tipo -> !tipo.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new OpcoesFiltro(tipos,
                Arrays.stream(StatusPedido.values()).map(s -> new Opcao(s.name(), s.getRotulo())).toList(),
                Arrays.stream(Agrupamento.values()).map(a -> new Opcao(a.name(), a.getRotulo())).toList(),
                Arrays.stream(OrdemRanking.values()).map(o -> new Opcao(o.name(), o.getRotulo())).toList(),
                padrao.inicio(), padrao.fim(), FiltroDashboard.LIMITE_DIAS);
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public Resumo resumo(FiltroDashboard filtro) {
        Map<String, String> tipos = tiposPorProduto();
        Indicadores atual = calcular(filtro, tipos).indicadores();

        FiltroDashboard anterior = filtro.periodoAnterior();
        Indicadores antes = calcular(anterior, tipos).indicadores();
        Comparacao comparacao = new Comparacao(anterior.inicio(), anterior.fim(), antes.faturamento(),
                antes.pedidosValidos(), antes.ticketMedio(),
                CalculadoraDashboard.variacao(atual.faturamento(), antes.faturamento()),
                CalculadoraDashboard.variacao(BigDecimal.valueOf(atual.pedidosValidos()),
                        BigDecimal.valueOf(antes.pedidosValidos())),
                CalculadoraDashboard.variacao(atual.ticketMedio(), antes.ticketMedio()));
        return new Resumo(aplicado(filtro), atual, comparacao, agora());
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public SerieVendas vendas(FiltroDashboard filtro, Agrupamento agrupamento) {
        Agrupamento escolhido = agrupamento == null ? Agrupamento.DIA : agrupamento;
        return new SerieVendas(aplicado(filtro), escolhido.name(),
                calcular(filtro, tiposPorProduto()).serie(escolhido), agora());
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public VendasPorCategoria categorias(FiltroDashboard filtro) {
        return new VendasPorCategoria(aplicado(filtro), calcular(filtro, tiposPorProduto()).categorias(), agora());
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public SituacaoPedidos situacao(FiltroDashboard filtro) {
        CalculadoraDashboard calculo = calcular(filtro, tiposPorProduto());
        return new SituacaoPedidos(aplicado(filtro), calculo.total(), calculo.etapas(), calculo.tempos(), agora());
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public RankingProdutos maisVendidos(FiltroDashboard filtro, Integer limite, OrdemRanking ordem) {
        int quantos = limite == null ? LIMITE_RANKING_PADRAO : limite;
        if (quantos < 1 || quantos > LIMITE_RANKING_MAXIMO) {
            throw new FiltroInvalidoException("limite", "O limite deve estar entre 1 e " + LIMITE_RANKING_MAXIMO + ".");
        }
        OrdemRanking criterio = ordem == null ? OrdemRanking.QUANTIDADE : ordem;
        return new RankingProdutos(aplicado(filtro), criterio.name(), quantos,
                calcular(filtro, tiposPorProduto()).ranking(quantos, criterio), agora());
    }

    private CalculadoraDashboard calcular(FiltroDashboard filtro, Map<String, String> tipos) {
        return new CalculadoraDashboard(
                pedidoRepository.findPorEntradaNoPeriodo(filtro.desde(), filtro.ate()), tipos, filtro);
    }

    // O pedido nao guarda a categoria do item; ela vem do cardapio pelo produtoId
    private Map<String, String> tiposPorProduto() {
        Map<String, String> tipos = new HashMap<>();
        produtoRepository.findAll().forEach(p -> tipos.put(p.getId(), p.getTipo() == null ? "" : p.getTipo().trim()));
        return tipos;
    }

    private static FiltroAplicado aplicado(FiltroDashboard filtro) {
        return new FiltroAplicado(filtro.inicio(), filtro.fim(), filtro.dias(), filtro.tipo(), filtro.status());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).withNano(0);
    }
}
