package com.umc.pediupatrao.dto.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contratos de resposta da API do dashboard (/api/dashboard/**).
 * Ficam juntos porque so fazem sentido juntos e mudam juntos.
 */
public final class DashboardDTOs {

    private DashboardDTOs() {
    }

    /** O filtro que de fato foi aplicado (com os padroes ja resolvidos). */
    public record FiltroAplicado(LocalDate inicio, LocalDate fim, long dias, String tipo, StatusPedido status) {
    }

    // ---------- GET /api/dashboard/filtros ----------

    public record Opcao(String valor, String rotulo) {
    }

    public record OpcoesFiltro(List<String> tipos, List<Opcao> status, List<Opcao> agrupamentos,
            List<Opcao> ordenacoes, LocalDate inicioPadrao, LocalDate fimPadrao, int limiteDias) {
    }

    // ---------- GET /api/dashboard/resumo (RF01) ----------

    public record Indicadores(BigDecimal faturamento, long pedidos, long pedidosValidos, BigDecimal ticketMedio,
            long itensVendidos, long cancelados, BigDecimal taxaCancelamento, BigDecimal descontoConcedido,
            long pedidosComDesconto, long emAndamento) {
    }

    public record Comparacao(LocalDate inicio, LocalDate fim, BigDecimal faturamento, long pedidosValidos,
            BigDecimal ticketMedio, BigDecimal variacaoFaturamento, BigDecimal variacaoPedidos,
            BigDecimal variacaoTicketMedio) {
    }

    public record Resumo(FiltroAplicado filtro, Indicadores indicadores, Comparacao periodoAnterior,
            LocalDateTime geradoEm) {
    }

    // ---------- GET /api/dashboard/vendas (RF02) ----------

    public record PontoVenda(String chave, String rotulo, long pedidos, BigDecimal faturamento, BigDecimal ticketMedio) {
    }

    public record SerieVendas(FiltroAplicado filtro, String agrupamento, List<PontoVenda> pontos,
            LocalDateTime geradoEm) {
    }

    // ---------- GET /api/dashboard/categorias (RF02) ----------

    public record Categoria(String tipo, long quantidade, BigDecimal faturamento, BigDecimal participacao) {
    }

    public record VendasPorCategoria(FiltroAplicado filtro, List<Categoria> categorias, LocalDateTime geradoEm) {
    }

    // ---------- GET /api/dashboard/status (RF03) ----------

    public record Etapa(StatusPedido status, String rotulo, long quantidade, BigDecimal percentual,
            BigDecimal valor) {
    }

    /** Minutos medios entre a entrada e cada marco; null quando nenhum pedido chegou ao marco. */
    public record Tempos(Long preparoMedioMin, Long saidaMediaMin, Long conclusaoMediaMin) {
    }

    public record SituacaoPedidos(FiltroAplicado filtro, long total, List<Etapa> etapas, Tempos tempos,
            LocalDateTime geradoEm) {
    }

    // ---------- GET /api/dashboard/produtos/mais-vendidos (RF04) ----------

    public record ProdutoVendido(int posicao, String produtoId, String nome, String tipo, long quantidade,
            BigDecimal faturamento, BigDecimal participacao) {
    }

    public record RankingProdutos(FiltroAplicado filtro, String ordenarPor, int limite, List<ProdutoVendido> produtos,
            LocalDateTime geradoEm) {
    }
}
