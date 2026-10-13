package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;

/**
 * "Quem e quando": usado na entrada, na saida, no desconto e no cancelamento do pedido.
 * Fica embutido no documento do pedido.
 */
public class Carimbo {

    private String usuario;
    private LocalDateTime momento;

    protected Carimbo() {
        // usado pelo Spring Data ao ler do MongoDB
    }

    public Carimbo(String usuario, LocalDateTime momento) {
        this.usuario = usuario;
        this.momento = momento;
    }

    public String getUsuario() {
        return usuario;
    }

    public LocalDateTime getMomento() {
        return momento;
    }
}
