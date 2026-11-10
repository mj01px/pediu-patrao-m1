package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.RankingProdutos;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Resumo;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.SerieVendas;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.SituacaoPedidos;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.VendasPorCategoria;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.dashboard.Agrupamento;
import com.umc.pediupatrao.service.dashboard.FiltroDashboard;
import com.umc.pediupatrao.service.dashboard.OrdemRanking;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API do Dashboard Administrativo. Todos os endpoints sao GET, somente leitura, e aceitam
 * os mesmos filtros (RF05): inicio e fim (yyyy-MM-dd), tipo (categoria do cardapio) e status.
 * Os numeros mudam a cada pedido, entao nada e guardado em cache pelo navegador.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/filtros")
    public ResponseEntity<OpcoesFiltro> filtros() {
        return semCache(dashboardService.opcoes());
    }

    @GetMapping("/resumo")
    public ResponseEntity<Resumo> resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) StatusPedido status) {
        return semCache(dashboardService.resumo(filtro(inicio, fim, tipo, status)));
    }

    @GetMapping("/vendas")
    public ResponseEntity<SerieVendas> vendas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Agrupamento agrupamento) {
        return semCache(dashboardService.vendas(filtro(inicio, fim, tipo, status), agrupamento));
    }

    @GetMapping("/categorias")
    public ResponseEntity<VendasPorCategoria> categorias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) StatusPedido status) {
        return semCache(dashboardService.categorias(filtro(inicio, fim, tipo, status)));
    }

    @GetMapping("/status")
    public ResponseEntity<SituacaoPedidos> status(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) StatusPedido status) {
        return semCache(dashboardService.situacao(filtro(inicio, fim, tipo, status)));
    }

    @GetMapping("/produtos/mais-vendidos")
    public ResponseEntity<RankingProdutos> maisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Integer limite,
            @RequestParam(required = false) OrdemRanking ordenarPor) {
        return semCache(dashboardService.maisVendidos(filtro(inicio, fim, tipo, status), limite, ordenarPor));
    }

    private FiltroDashboard filtro(LocalDate inicio, LocalDate fim, String tipo, StatusPedido status) {
        return dashboardService.filtro(inicio, fim, tipo, status);
    }

    private static <T> ResponseEntity<T> semCache(T corpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(corpo);
    }
}
