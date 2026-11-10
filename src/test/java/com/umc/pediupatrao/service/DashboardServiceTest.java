package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.OpcoesFiltro;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Resumo;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.erro.FiltroInvalidoException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.dashboard.FiltroDashboard;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/** DashboardService: segunda barreira de acesso (@PreAuthorize) e consulta ao banco por periodo. */
@SpringJUnitConfig(DashboardServiceTest.Contexto.class)
class DashboardServiceTest {

    static final PedidoRepository PEDIDOS = mock(PedidoRepository.class);
    static final ProdutoRepository PRODUTOS = mock(ProdutoRepository.class);

    @Configuration
    @EnableMethodSecurity
    static class Contexto {
        @Bean
        DashboardService dashboardService() {
            Clock relogio = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
            return new DashboardService(PEDIDOS, PRODUTOS, relogio);
        }
    }

    @Autowired
    private DashboardService dashboard;

    @BeforeEach
    void limpar() {
        reset(PEDIDOS, PRODUTOS);
        when(PEDIDOS.findPorEntradaNoPeriodo(any(), any())).thenReturn(List.of());
        when(PRODUTOS.findAll()).thenReturn(List.of(produto("Pizza"), produto("Bebida"), produto("Pizza"), produto(" ")));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteEBarradoNoService() {
        FiltroDashboard filtro = dashboard.filtro(null, null, null, null);
        assertThatThrownBy(() -> dashboard.resumo(filtro)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> dashboard.opcoes()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void resumoConsultaOPeriodoEOPeriodoAnterior() {
        FiltroDashboard filtro = dashboard.filtro(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10), null, null);
        Resumo resumo = dashboard.resumo(filtro);

        verify(PEDIDOS).findPorEntradaNoPeriodo(LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 11, 0, 0));
        verify(PEDIDOS).findPorEntradaNoPeriodo(LocalDateTime.of(2026, 10, 22, 0, 0), LocalDateTime.of(2026, 11, 1, 0, 0));
        assertThat(resumo.periodoAnterior().variacaoFaturamento()).isNull(); // sem base para comparar
        assertThat(resumo.geradoEm()).isEqualTo(LocalDateTime.of(2026, 11, 10, 12, 0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void opcoesListamCategoriasSemRepetirEPeriodoPadraoNoFusoDaLoja() {
        OpcoesFiltro opcoes = dashboard.opcoes();

        assertThat(opcoes.tipos()).containsExactly("Bebida", "Pizza");
        assertThat(opcoes.fimPadrao()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(opcoes.inicioPadrao()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(opcoes.status()).hasSize(7);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void limiteDoRankingForaDaFaixaEFiltroInvalido() {
        FiltroDashboard filtro = dashboard.filtro(null, null, null, null);
        assertThatThrownBy(() -> dashboard.maisVendidos(filtro, 0, null)).isInstanceOf(FiltroInvalidoException.class);
        assertThatThrownBy(() -> dashboard.maisVendidos(filtro, 51, null)).isInstanceOf(FiltroInvalidoException.class);
        assertThat(dashboard.maisVendidos(filtro, null, null).limite()).isEqualTo(10);
    }

    private static Produto produto(String tipo) {
        Produto produto = new Produto();
        produto.setId(tipo + Math.random());
        produto.setTipo(tipo);
        return produto;
    }
}
