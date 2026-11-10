package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Comparacao;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.FiltroAplicado;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Indicadores;
import com.umc.pediupatrao.dto.dashboard.DashboardDTOs.Resumo;
import com.umc.pediupatrao.seguranca.RespostaNegadaApi;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.dashboard.FiltroDashboard;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato HTTP da /api/dashboard: permissoes, validacao dos filtros e formato de erro. */
@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, RespostaNegadaApi.class})
class DashboardApiTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService dashboardService;
    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @BeforeEach
    void filtroReal() {
        // O controller monta o filtro pelo service; aqui ele usa a regra verdadeira com uma data fixa
        when(dashboardService.filtro(any(), any(), any(), any())).thenAnswer(chamada ->
                FiltroDashboard.de(chamada.getArgument(0), chamada.getArgument(1), chamada.getArgument(2),
                        chamada.getArgument(3), HOJE));
        when(dashboardService.resumo(any())).thenAnswer(chamada -> {
            FiltroDashboard f = chamada.getArgument(0);
            return new Resumo(new FiltroAplicado(f.inicio(), f.fim(), f.dias(), f.tipo(), f.status()),
                    new Indicadores(new BigDecimal("157.71"), 3, 2, new BigDecimal("78.86"), 4, 1,
                            new BigDecimal("33.3"), new BigDecimal("4.99"), 1, 1),
                    new Comparacao(f.periodoAnterior().inicio(), f.periodoAnterior().fim(), new BigDecimal("100.00"),
                            2, new BigDecimal("50.00"), new BigDecimal("57.7"), BigDecimal.ZERO.setScale(1),
                            new BigDecimal("57.7")),
                    LocalDateTime.of(2026, 11, 10, 20, 15));
        });
    }

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("nao-autenticado"));
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoVeODashboard() throws Exception {
        mvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("acesso-negado"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteVeOResumoComOPeriodoPadrao() throws Exception {
        mvc.perform(get("/api/dashboard/resumo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.filtro.inicio").value("2026-10-12"))
                .andExpect(jsonPath("$.filtro.fim").value("2026-11-10"))
                .andExpect(jsonPath("$.filtro.dias").value(30))
                .andExpect(jsonPath("$.indicadores.faturamento").value(157.71))
                .andExpect(jsonPath("$.periodoAnterior.variacaoFaturamento").value(57.7));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminVeOResumoComFiltros() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")
                        .param("inicio", "2026-10-01").param("fim", "2026-10-31")
                        .param("tipo", "Pizza").param("status", "FINALIZADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtro.dias").value(31))
                .andExpect(jsonPath("$.filtro.tipo").value("Pizza"))
                .andExpect(jsonPath("$.filtro.status").value("FINALIZADO"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void periodoInvertidoRespondeFiltroInvalido() throws Exception {
        mvc.perform(get("/api/dashboard/vendas").param("inicio", "2026-11-10").param("fim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("filtro-invalido"))
                .andExpect(jsonPath("$.parametro").value("inicio"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void dataEmFormatoErradoRespondeFormatoInvalido() throws Exception {
        mvc.perform(get("/api/dashboard/resumo").param("inicio", "10/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("formato-invalido"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void statusInexistenteRespondeFormatoInvalido() throws Exception {
        mvc.perform(get("/api/dashboard/status").param("status", "VOANDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("formato-invalido"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void agrupamentoInexistenteRespondeFormatoInvalido() throws Exception {
        mvc.perform(get("/api/dashboard/vendas").param("agrupamento", "ANO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("formato-invalido"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void dashboardESomenteLeitura() throws Exception {
        mvc.perform(post("/api/dashboard/resumo").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
