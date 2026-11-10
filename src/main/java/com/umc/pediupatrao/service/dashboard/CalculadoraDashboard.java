package com.umc.pediupatrao.service.dashboard;

import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Categoria;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Etapa;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Indicadores;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.PontoVenda;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.ProdutoVendido;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Tempos;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.MarcoPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula os numeros do dashboard a partir dos pedidos de um periodo. Nao acessa banco:
 * recebe os pedidos ja carregados e o tipo de cada produto, o que permite testar
 * todas as regras sem MongoDB.
 *
 * Regras de negocio dos indicadores:
 *  - Pedido CANCELADO conta em "pedidos" e "cancelados", mas nunca entra em faturamento,
 *    ticket medio, itens vendidos, series, categorias ou ranking.
 *  - Faturamento e o valor liquido (ja com desconto). Com filtro de categoria, cada item
 *    da categoria entra com o seu subtotal menos o mesmo percentual de desconto do pedido.
 *  - Os valores vem do proprio pedido (preco "fotografado" na venda), nunca do cardapio atual.
 */
public final class CalculadoraDashboard {

    static final String SEM_CATEGORIA = "Sem categoria";

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MMM/yy", BRASIL);

    /** Um item vendido, com a categoria resolvida e o valor liquido (com o desconto do pedido). */
    record Venda(ItemPedido item, String tipo, BigDecimal liquido) {
    }

    /** Um pedido que passou nos filtros, com os itens que contam e o valor que ele soma. */
    record Considerado(Pedido pedido, List<Venda> vendas, BigDecimal valor, BigDecimal bruto) {
        boolean valido() {
            return pedido.getStatus() != StatusPedido.CANCELADO;
        }
    }

    private final FiltroDashboard filtro;
    private final List<Considerado> considerados;

    public CalculadoraDashboard(List<Pedido> pedidos, Map<String, String> tipoPorProduto, FiltroDashboard filtro) {
        this.filtro = filtro;
        this.considerados = pedidos.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() != null)
                .filter(p -> filtro.status() == null || p.getStatus() == filtro.status())
                .map(p -> considerar(p, tipoPorProduto))
                .filter(Objects::nonNull)
                .toList();
    }

    private Considerado considerar(Pedido pedido, Map<String, String> tipoPorProduto) {
        BigDecimal fator = CEM.subtract(nulo(pedido.getPercentualDesconto())).divide(CEM, 6, RoundingMode.HALF_UP);
        List<Venda> vendas = pedido.getItens().stream()
                .map(item -> new Venda(item, tipoDe(item, tipoPorProduto),
                        item.getSubtotal().multiply(fator).setScale(2, RoundingMode.HALF_UP)))
                .filter(venda -> filtro.aceitaTipo(venda.tipo()))
                .toList();
        if (vendas.isEmpty() && filtro.tipo() != null) {
            return null; // pedido sem nenhum item da categoria filtrada
        }
        BigDecimal bruto = soma(vendas, v -> v.item().getSubtotal());
        // Sem filtro de categoria o valor e o total gravado no pedido (fonte da verdade)
        BigDecimal valor = filtro.tipo() == null ? nulo(pedido.getTotal()) : soma(vendas, Venda::liquido);
        return new Considerado(pedido, vendas, valor, bruto);
    }

    private static String tipoDe(ItemPedido item, Map<String, String> tipoPorProduto) {
        String tipo = tipoPorProduto.get(item.getProdutoId());
        if (tipo == null) {
            return SEM_CATEGORIA; // produto removido do cardapio depois da venda
        }
        return tipo.isBlank() ? "Outros" : tipo;
    }

    // ---------------- RF01 ----------------

    public Indicadores indicadores() {
        List<Considerado> validos = validos();
        BigDecimal faturamento = soma(validos, Considerado::valor);
        long cancelados = considerados.size() - validos.size();
        return new Indicadores(
                faturamento,
                considerados.size(),
                validos.size(),
                media(faturamento, validos.size()),
                validos.stream().flatMap(c -> c.vendas().stream()).mapToLong(v -> v.item().getQuantidade()).sum(),
                cancelados,
                percentual(cancelados, considerados.size()),
                soma(validos, c -> c.bruto().subtract(c.valor()).max(BigDecimal.ZERO)),
                validos.stream().filter(c -> nulo(c.pedido().getPercentualDesconto()).signum() > 0).count(),
                validos.stream().filter(c -> c.pedido().getStatus() != StatusPedido.FINALIZADO).count());
    }

    /** Variacao percentual de "atual" sobre "anterior"; null quando nao ha base (anterior zero). */
    public static BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return atual.subtract(anterior).multiply(CEM).divide(anterior, 1, RoundingMode.HALF_UP);
    }

    // ---------------- RF02 ----------------

    public List<PontoVenda> serie(Agrupamento agrupamento) {
        Map<String, String> rotulos = new LinkedHashMap<>();
        switch (agrupamento) {
            case HORA -> {
                for (int hora = 0; hora < 24; hora++) {
                    rotulos.put(String.format("%02d", hora), hora + "h");
                }
            }
            case DIA -> filtro.inicio().datesUntil(filtro.fim().plusDays(1))
                    .forEach(dia -> rotulos.put(dia.toString(), DIA_MES.format(dia)));
            case SEMANA -> {
                LocalDate semana = segunda(filtro.inicio());
                while (!semana.isAfter(filtro.fim())) {
                    rotulos.put(semana.toString(), "Sem. " + DIA_MES.format(semana));
                    semana = semana.plusWeeks(1);
                }
            }
            case MES -> {
                YearMonth mes = YearMonth.from(filtro.inicio());
                while (!mes.isAfter(YearMonth.from(filtro.fim()))) {
                    rotulos.put(mes.toString(), MES_ANO.format(mes));
                    mes = mes.plusMonths(1);
                }
            }
        }

        Map<String, List<Considerado>> porChave = validos().stream()
                .filter(c -> c.pedido().getEntrada() != null && c.pedido().getEntrada().getMomento() != null)
                .collect(Collectors.groupingBy(c -> chave(agrupamento, c.pedido().getEntrada().getMomento())));

        return rotulos.entrySet().stream().map(entrada -> {
            List<Considerado> doPonto = porChave.getOrDefault(entrada.getKey(), List.of());
            BigDecimal faturamento = soma(doPonto, Considerado::valor);
            return new PontoVenda(entrada.getKey(), entrada.getValue(), doPonto.size(), faturamento,
                    media(faturamento, doPonto.size()));
        }).toList();
    }

    private static String chave(Agrupamento agrupamento, LocalDateTime momento) {
        return switch (agrupamento) {
            case HORA -> String.format("%02d", momento.getHour());
            case DIA -> momento.toLocalDate().toString();
            case SEMANA -> segunda(momento.toLocalDate()).toString();
            case MES -> YearMonth.from(momento).toString();
        };
    }

    private static LocalDate segunda(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public List<Categoria> categorias() {
        List<Venda> vendas = vendasValidas();
        BigDecimal total = soma(vendas, Venda::liquido);
        return vendas.stream()
                .collect(Collectors.groupingBy(Venda::tipo, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(grupo -> {
                    BigDecimal faturamento = soma(grupo.getValue(), Venda::liquido);
                    return new Categoria(grupo.getKey(),
                            grupo.getValue().stream().mapToLong(v -> v.item().getQuantidade()).sum(),
                            faturamento, participacao(faturamento, total));
                })
                .sorted(Comparator.comparing(Categoria::faturamento).reversed().thenComparing(Categoria::tipo))
                .toList();
    }

    // ---------------- RF03 ----------------

    public long total() {
        return considerados.size();
    }

    public List<Etapa> etapas() {
        Map<StatusPedido, List<Considerado>> porStatus = considerados.stream()
                .collect(Collectors.groupingBy(c -> c.pedido().getStatus()));
        List<Etapa> etapas = new ArrayList<>();
        for (StatusPedido status : StatusPedido.values()) {
            List<Considerado> daEtapa = porStatus.getOrDefault(status, List.of());
            etapas.add(new Etapa(status, status.getRotulo(), daEtapa.size(),
                    percentual(daEtapa.size(), considerados.size()), soma(daEtapa, Considerado::valor)));
        }
        return etapas;
    }

    public Tempos tempos() {
        return new Tempos(
                minutosMedios(m -> m.getStatus() == StatusPedido.PRONTO),
                minutosMedios(m -> m.getStatus().marcaSaida()),
                minutosMedios(m -> m.getStatus() == StatusPedido.FINALIZADO));
    }

    private Long minutosMedios(java.util.function.Predicate<MarcoPedido> marco) {
        List<Long> minutos = validos().stream()
                .filter(c -> c.pedido().getEntrada() != null && c.pedido().getEntrada().getMomento() != null)
                .map(c -> c.pedido().getLinhaDoTempo().stream()
                        .filter(m -> m.getCarimbo() != null && m.getCarimbo().getMomento() != null)
                        .filter(marco)
                        .findFirst()
                        .map(m -> Duration.between(c.pedido().getEntrada().getMomento(),
                                m.getCarimbo().getMomento()).toMinutes())
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        if (minutos.isEmpty()) {
            return null;
        }
        return Math.round(minutos.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    // ---------------- RF04 ----------------

    public List<ProdutoVendido> ranking(int limite, OrdemRanking ordem) {
        List<Venda> vendas = vendasValidas();
        BigDecimal total = soma(vendas, Venda::liquido);

        Comparator<ProdutoVendido> porQuantidade = Comparator.comparingLong(ProdutoVendido::quantidade).reversed();
        Comparator<ProdutoVendido> porFaturamento = Comparator.comparing(ProdutoVendido::faturamento).reversed();
        Comparator<ProdutoVendido> criterio = ordem == OrdemRanking.FATURAMENTO
                ? porFaturamento.thenComparing(porQuantidade)
                : porQuantidade.thenComparing(porFaturamento);

        List<ProdutoVendido> agrupados = vendas.stream()
                .collect(Collectors.groupingBy(v -> v.item().getProdutoId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(grupo -> {
                    Venda ultima = grupo.get(grupo.size() - 1);
                    BigDecimal faturamento = soma(grupo, Venda::liquido);
                    return new ProdutoVendido(0, ultima.item().getProdutoId(), ultima.item().getDescricao(),
                            ultima.tipo(), grupo.stream().mapToLong(v -> v.item().getQuantidade()).sum(),
                            faturamento, participacao(faturamento, total));
                })
                .sorted(criterio.thenComparing(ProdutoVendido::nome, Comparator.nullsLast(String::compareTo)))
                .limit(limite)
                .toList();

        List<ProdutoVendido> numerados = new ArrayList<>();
        for (int i = 0; i < agrupados.size(); i++) {
            ProdutoVendido p = agrupados.get(i);
            numerados.add(new ProdutoVendido(i + 1, p.produtoId(), p.nome(), p.tipo(), p.quantidade(),
                    p.faturamento(), p.participacao()));
        }
        return numerados;
    }

    // ---------------- utilitarios ----------------

    private List<Considerado> validos() {
        return considerados.stream().filter(Considerado::valido).toList();
    }

    private List<Venda> vendasValidas() {
        return validos().stream().flatMap(c -> c.vendas().stream()).toList();
    }

    private static <T> BigDecimal soma(List<T> itens, Function<T, BigDecimal> valor) {
        return itens.stream().map(valor).map(CalculadoraDashboard::nulo)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal media(BigDecimal soma, long quantidade) {
        if (quantidade == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return soma.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentual(long parte, long todo) {
        if (todo == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(parte).multiply(CEM).divide(BigDecimal.valueOf(todo), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal participacao(BigDecimal parte, BigDecimal todo) {
        if (todo.signum() == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return parte.multiply(CEM).divide(todo, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
