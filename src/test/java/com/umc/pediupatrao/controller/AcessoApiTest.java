package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.seguranca.RespostaNegadaApi;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Regras de rota da cadeia /api/** do SecurityConfig, com requisicoes HTTP simuladas. */
@WebMvcTest(controllers = {PedidoController.class, UsuarioController.class,
        AuditoriaController.class, ClienteController.class})
@Import({SecurityConfig.class, RespostaNegadaApi.class})
class AcessoApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PedidoService pedidoService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private AuditoriaService auditoriaService;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void semLoginRecebe401EmProblemDetail() throws Exception {
        mvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("nao-autenticado"));
    }

    @Test
    void apiDeUsuariosNaoEMaisPublica() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoSePromoveAAdmin() throws Exception {
        mvc.perform(json(post("/api/usuarios"), "{\"username\":\"novo\",\"senha\":\"12345678\",\"perfil\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("acesso-negado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoDaDesconto() throws Exception {
        mvc.perform(json(put("/api/pedidos/1/desconto"), "{\"percentual\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoCancela() throws Exception {
        mvc.perform(json(post("/api/pedidos/1/cancelamento"), "{\"motivo\":\"cliente desistiu\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteDaDesconto() throws Exception {
        when(pedidoService.aplicarDesconto(eq("1"), any())).thenReturn(null);
        mvc.perform(json(put("/api/pedidos/1/desconto"), "{\"percentual\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void descontoAcimaDoLimiteVolta400ComOsErros() throws Exception {
        mvc.perform(json(put("/api/pedidos/1/desconto"), "{\"percentual\":25}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("dados-invalidos"))
                .andExpect(jsonPath("$.erros[0]").value("percentual: o limite é 20%"));
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void chamadaDeTelaSemCsrfEBloqueada() throws Exception {
        mvc.perform(put("/api/pedidos/1/desconto").contentType(MediaType.APPLICATION_JSON).content("{\"percentual\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("csrf-invalido"));
    }

    @Test
    void credencialBasicInvalidaRecebe401() throws Exception {
        mvc.perform(get("/api/pedidos").with(httpBasic("gerente", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoLeAuditoria() throws Exception {
        mvc.perform(get("/api/auditoria")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ninguemApagaAuditoria() throws Exception {
        mvc.perform(delete("/api/auditoria").with(csrf())).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoVeClientes() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void rotaDeApiNaoPrevistaENegada() throws Exception {
        mvc.perform(get("/api/qualquer-coisa")).andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }
}
