package com.umc.pediupatrao.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.seguranca.RespostaNegadaApi;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Regras de rota da cadeia de telas do SecurityConfig. */
@WebMvcTest(controllers = HomeController.class)
@Import({SecurityConfig.class, RespostaNegadaApi.class})
class AcessoTelasTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private ProdutoService produtoService;
    @MockitoBean
    private PedidoService pedidoService;
    @MockitoBean
    private Clock relogio;
    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoAbreOPainelAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoAbreOPainelAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAbreOPainelAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk());
    }
}
