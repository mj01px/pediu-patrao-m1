package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.umc.pediupatrao.dto.UsuarioFormDTO;
import com.umc.pediupatrao.erro.NaoEncontradoException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EventoAuditoriaRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import com.umc.pediupatrao.seguranca.EncerradorDeSessoes;
import com.umc.pediupatrao.seguranca.UsuarioLogado;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * O @PreAuthorize barra o perfil errado DENTRO do service, sem passar por rota nem tela.
 * Prova que a autorizacao esta no backend.
 */
@SpringJUnitConfig(SegurancaMetodosTest.Contexto.class)
class SegurancaMetodosTest {

    @Configuration
    @EnableMethodSecurity
    static class Contexto {

        @Bean
        PedidoService pedidoService() {
            return new PedidoService(mock(PedidoRepository.class), mock(ProdutoRepository.class),
                    mock(ClienteRepository.class), mock(UsuarioLogado.class), mock(ApplicationEventPublisher.class));
        }

        @Bean
        UsuarioService usuarioService() {
            return new UsuarioService(mock(UsuarioRepository.class), mock(PasswordEncoder.class),
                    mock(UsuarioLogado.class), mock(ApplicationEventPublisher.class), mock(EncerradorDeSessoes.class));
        }

        @Bean
        AuditoriaService auditoriaService() {
            return new AuditoriaService(mock(EventoAuditoriaRepository.class), mock(UsuarioLogado.class));
        }
    }

    @Autowired
    private PedidoService pedidos;
    @Autowired
    private UsuarioService usuarios;
    @Autowired
    private AuditoriaService auditoria;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoDaDesconto() {
        assertThatThrownBy(() -> pedidos.aplicarDesconto("1", BigDecimal.TEN)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoCancela() {
        assertThatThrownBy(() -> pedidos.cancelar("1", "motivo qualquer")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoCancela() {
        assertThatThrownBy(() -> pedidos.cancelar("1", "motivo qualquer")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerenteNaoAdministraUsuarios() {
        assertThatThrownBy(() -> usuarios.criar(new UsuarioFormDTO())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoLeAuditoria() {
        assertThatThrownBy(() -> auditoria.pesquisar(null, null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePassaPelaSeguranca() {
        // Passou da autorizacao; so falha porque o pedido nao existe no mock
        assertThatThrownBy(() -> pedidos.cancelar("inexistente", "Cliente desistiu do pedido"))
                .isInstanceOf(NaoEncontradoException.class);
    }
}
