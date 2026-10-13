package com.umc.pediupatrao.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class StatusPedidoTest {

    @ParameterizedTest(name = "{0} -> {1} permitido? {2}")
    @CsvSource({
        "RECEBIDO,          EM_PREPARACAO,     true",
        "EM_PREPARACAO,     PRONTO,            true",
        "PRONTO,            SAIU_PARA_ENTREGA, true",
        "PRONTO,            RETIRADO,          true",
        "SAIU_PARA_ENTREGA, FINALIZADO,        true",
        "RETIRADO,          FINALIZADO,        true",
        "EM_PREPARACAO,     FINALIZADO,        false",
        "RECEBIDO,          PRONTO,            false",
        "PRONTO,            RECEBIDO,          false",
        "RECEBIDO,          CANCELADO,         false",
        "CANCELADO,         RECEBIDO,          false",
        "FINALIZADO,        RECEBIDO,          false"
    })
    void respeitaOFluxo(StatusPedido origem, StatusPedido destino, boolean esperado) {
        assertThat(origem.permite(destino)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"RECEBIDO", "EM_PREPARACAO", "PRONTO"})
    void antesDaSaidaEstaEmAberto(StatusPedido status) {
        assertThat(status.emAberto()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"})
    void depoisDaSaidaNaoEstaEmAberto(StatusPedido status) {
        assertThat(status.emAberto()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"FINALIZADO", "CANCELADO"})
    void estadosFinaisNaoTemSeguinte(StatusPedido status) {
        assertThat(status.seguintes()).isEmpty();
    }
}
