package com.umc.pediupatrao.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.umc.pediupatrao.erro.NegocioException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regras do pedido testadas direto na entidade: sem Spring, sem banco, sem mocks. */
class PedidoTest {

    private static final Carimbo ATENDENTE = new Carimbo("atendente", LocalDateTime.of(2026, 9, 23, 14, 0));
    private static final Carimbo GERENTE = new Carimbo("gerente", LocalDateTime.of(2026, 9, 23, 14, 30));

    private Pedido pedido;

    @BeforeEach
    void abrirPedido() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        // 2 x 49,90 + 1 x 13,00 = 112,80
        pedido = Pedido.abrir(cliente,
                List.of(new ItemPedido(produto("Calabresa", 49.90), 2), new ItemPedido(produto("Guaraná", 13.00), 1)),
                "sem cebola", ATENDENTE);
    }

    @Test
    void abreComTotalCalculadoEEntradaCarimbada() {
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.RECEBIDO);
        assertThat(pedido.getTotal()).isEqualByComparingTo("112.80");
        assertThat(pedido.getEntrada().getUsuario()).isEqualTo("atendente");
        assertThat(pedido.getLinhaDoTempo()).hasSize(1);
    }

    @Test
    void naoAbrePedidoSemItens() {
        assertThatThrownBy(() -> Pedido.abrir(new Cliente(), List.of(), null, ATENDENTE))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void saidaRegistraQuemEQuando() {
        pedido.avancarPara(StatusPedido.EM_PREPARACAO, ATENDENTE);
        pedido.avancarPara(StatusPedido.PRONTO, ATENDENTE);
        pedido.avancarPara(StatusPedido.RETIRADO, GERENTE);

        assertThat(pedido.getSaida().getUsuario()).isEqualTo("gerente");
        assertThat(pedido.getSaida().getMomento()).isEqualTo(GERENTE.getMomento());
        assertThat(pedido.getLinhaDoTempo()).extracting(MarcoPedido::getStatus)
                .containsExactly(StatusPedido.RECEBIDO, StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO, StatusPedido.RETIRADO);
    }

    @Test
    void naoPulaEtapa() {
        pedido.avancarPara(StatusPedido.EM_PREPARACAO, ATENDENTE);

        assertThatThrownBy(() -> pedido.avancarPara(StatusPedido.FINALIZADO, ATENDENTE))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Não é possível ir de");
    }

    @Test
    void descontoRecalculaOTotal() {
        pedido.aplicarDesconto(new BigDecimal("10"), GERENTE);

        assertThat(pedido.getDesconto()).isEqualByComparingTo("11.28");
        assertThat(pedido.getTotal()).isEqualByComparingTo("101.52");
        assertThat(pedido.getCarimboDesconto().getUsuario()).isEqualTo("gerente");
    }

    @Test
    void descontoAcimaDe20PorCentoERecusado() {
        assertThatThrownBy(() -> pedido.aplicarDesconto(new BigDecimal("20.01"), GERENTE))
                .isInstanceOf(NegocioException.class);
        assertThat(pedido.getTotal()).isEqualByComparingTo("112.80");
    }

    @Test
    void descontoDepoisDaSaidaERecusado() {
        levarAteASaida();

        assertThatThrownBy(() -> pedido.aplicarDesconto(new BigDecimal("5"), GERENTE))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void cancelamentoExigeMotivo() {
        assertThatThrownBy(() -> pedido.cancelar("curto", GERENTE))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void cancelamentoPreservaValoresETravaOPedido() {
        pedido.cancelar("Cliente desistiu por telefone", GERENTE);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedido.getCancelamento().getUsuario()).isEqualTo("gerente");
        assertThat(pedido.getTotal()).isEqualByComparingTo("112.80");
        assertThatThrownBy(() -> pedido.avancarPara(StatusPedido.EM_PREPARACAO, ATENDENTE))
                .isInstanceOf(NegocioException.class);
        assertThatThrownBy(() -> pedido.aplicarDesconto(BigDecimal.ONE, GERENTE))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void naoCancelaDepoisDaSaida() {
        levarAteASaida();

        assertThatThrownBy(() -> pedido.cancelar("Tentativa depois da entrega", GERENTE))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    void cancelarNaoEUmaEtapaDoFluxoNormal() {
        assertThatThrownBy(() -> pedido.avancarPara(StatusPedido.CANCELADO, GERENTE))
                .isInstanceOf(NegocioException.class);
    }

    private void levarAteASaida() {
        pedido.avancarPara(StatusPedido.EM_PREPARACAO, ATENDENTE);
        pedido.avancarPara(StatusPedido.PRONTO, ATENDENTE);
        pedido.avancarPara(StatusPedido.SAIU_PARA_ENTREGA, ATENDENTE);
    }

    private static Produto produto(String nome, double preco) {
        Produto produto = new Produto();
        produto.setId(nome);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }
}
