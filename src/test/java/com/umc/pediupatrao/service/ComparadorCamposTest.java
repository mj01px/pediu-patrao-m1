package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Mudanca;
import org.junit.jupiter.api.Test;

class ComparadorCamposTest {

    private final ComparadorCampos<Cliente> comparador = ComparadorCampos.para(Cliente.class)
            .campo("logradouro", Cliente::getLogradouro)
            .campo("bairro", Cliente::getBairro)
            .sensivel("telefone", Cliente::getTelefone);

    @Test
    void registraSoOQueMudouComValorAnteriorENovo() {
        Cliente antes = cliente("Rua das Flores", "Centro", "(11) 98765-4321");
        Cliente depois = cliente("Av. Brasil", "Centro", "(11) 98765-4321");

        assertThat(comparador.comparar(antes, depois).lista())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.getCampo()).isEqualTo("logradouro");
                    assertThat(m.getDe()).isEqualTo("Rua das Flores");
                    assertThat(m.getPara()).isEqualTo("Av. Brasil");
                });
    }

    @Test
    void telefoneVaiMascarado() {
        Cliente antes = cliente("Rua A", "Centro", "(11) 98765-4321");
        Cliente depois = cliente("Rua A", "Centro", "(11) 91234-0000");

        Mudanca mudanca = comparador.comparar(antes, depois).lista().get(0);

        assertThat(mudanca.getDe()).isEqualTo("*******4321").doesNotContain("98765");
        assertThat(mudanca.getPara()).isEqualTo("*******0000");
    }

    @Test
    void inclusaoMostraCamposPreenchidosComoNovos() {
        assertThat(comparador.comparar(null, cliente("Rua A", null, "11999990000")).lista())
                .extracting(Mudanca::getCampo)
                .containsExactly("logradouro", "telefone");
    }

    private static Cliente cliente(String logradouro, String bairro, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setLogradouro(logradouro);
        cliente.setBairro(bairro);
        cliente.setTelefone(telefone);
        return cliente;
    }
}
