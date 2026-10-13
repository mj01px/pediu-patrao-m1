package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.NovoPedidoDTO;
import com.umc.pediupatrao.entity.Carimbo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.erro.NegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.seguranca.UsuarioLogado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Orquestracao do PedidoService: preco vem do cardapio e cada operacao gera evento de auditoria. */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioLogado usuarioLogado;
    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private PedidoService service;

    @Test
    void registraComPrecoDoCardapioEPublicaEvento() {
        prepararCliente();
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto("p1", 49.90, true)));
        when(usuarioLogado.carimbo()).thenReturn(new Carimbo("atendente", LocalDateTime.now()));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Pedido pedido = service.registrar(new NovoPedidoDTO("c1", List.of(new NovoPedidoDTO.ItemDTO("p1", 3)), null));

        assertThat(pedido.getTotal()).isEqualByComparingTo("149.70");
        assertThat(pedido.getEntrada().getUsuario()).isEqualTo("atendente");

        ArgumentCaptor<RegistroAlteracao> evento = ArgumentCaptor.forClass(RegistroAlteracao.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(TipoEvento.PEDIDO_REGISTRADO);
        assertThat(evento.getValue().mudancas()).extracting("campo").contains("cliente", "itens", "total", "status");
    }

    @Test
    void recusaProdutoForaDeVenda() {
        prepararCliente();
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto("p1", 49.90, false)));

        assertThatThrownBy(() -> service.registrar(
                new NovoPedidoDTO("c1", List.of(new NovoPedidoDTO.ItemDTO("p1", 1)), null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("não está disponível");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void descontoInvalidoNaoSalvaNemAudita() {
        Pedido pedido = Pedido.abrir(cliente(), List.of(new ItemPedido(produto("p1", 10.0, true), 1)),
                null, new Carimbo("atendente", LocalDateTime.now()));
        when(pedidoRepository.findById("x")).thenReturn(Optional.of(pedido));
        when(usuarioLogado.carimbo()).thenReturn(new Carimbo("gerente", LocalDateTime.now()));

        assertThatThrownBy(() -> service.aplicarDesconto("x", new BigDecimal("25")))
                .isInstanceOf(NegocioException.class);
        verify(pedidoRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any());
    }

    private void prepararCliente() {
        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente()));
    }

    private static Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        return cliente;
    }

    private static Produto produto(String id, double preco, boolean ativo) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome("Pizza " + id);
        produto.setPreco(preco);
        produto.setAtivo(ativo);
        return produto;
    }
}
