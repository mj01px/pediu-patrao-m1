package com.umc.pediupatrao.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Etapas do pedido. O mapa FLUXO e a unica fonte da verdade sobre
 * quais transicoes sao permitidas (RF03).
 */
public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARACAO("Em preparação"),
    PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    RETIRADO("Retirado"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado");

    private static final Map<StatusPedido, Set<StatusPedido>> FLUXO = new EnumMap<>(StatusPedido.class);

    static {
        FLUXO.put(RECEBIDO, EnumSet.of(EM_PREPARACAO));
        FLUXO.put(EM_PREPARACAO, EnumSet.of(PRONTO));
        FLUXO.put(PRONTO, EnumSet.of(SAIU_PARA_ENTREGA, RETIRADO));
        FLUXO.put(SAIU_PARA_ENTREGA, EnumSet.of(FINALIZADO));
        FLUXO.put(RETIRADO, EnumSet.of(FINALIZADO));
        // FINALIZADO e CANCELADO sao finais. CANCELADO nao entra no fluxo: tem operacao propria.
        FLUXO.put(FINALIZADO, EnumSet.noneOf(StatusPedido.class));
        FLUXO.put(CANCELADO, EnumSet.noneOf(StatusPedido.class));
    }

    private final String rotulo;

    StatusPedido(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public Set<StatusPedido> seguintes() {
        return Collections.unmodifiableSet(FLUXO.get(this));
    }

    public boolean permite(StatusPedido destino) {
        return FLUXO.get(this).contains(destino);
    }

    /** SAIU_PARA_ENTREGA e RETIRADO representam a saida do pedido da loja. */
    public boolean marcaSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }

    /** Ainda dentro da loja: so aqui cabe desconto ou cancelamento. */
    public boolean emAberto() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
