package com.umc.pediupatrao.entity;

/** Um ponto da linha do tempo do pedido: a etapa e o carimbo de quem levou o pedido ate ela. */
public class MarcoPedido {

    private StatusPedido status;
    private Carimbo carimbo;

    protected MarcoPedido() {
        // usado pelo Spring Data ao ler do MongoDB
    }

    public MarcoPedido(StatusPedido status, Carimbo carimbo) {
        this.status = status;
        this.carimbo = carimbo;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public Carimbo getCarimbo() {
        return carimbo;
    }
}
