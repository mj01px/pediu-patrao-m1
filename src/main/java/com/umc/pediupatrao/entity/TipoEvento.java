package com.umc.pediupatrao.entity;

/** Tipos de evento gravados na trilha de auditoria. */
public enum TipoEvento {
    PEDIDO_REGISTRADO("INCLUSÃO DE PEDIDO"),
    PEDIDO_AVANCOU("MUDANÇA DE STATUS"),
    PEDIDO_COM_DESCONTO("DESCONTO APLICADO"),
    PEDIDO_CANCELADO("CANCELAMENTO DE PEDIDO"),
    CLIENTE_CADASTRADO("INCLUSÃO DE CLIENTE"),
    CLIENTE_ALTERADO("ALTERAÇÃO DE CLIENTE"),
    CLIENTE_REMOVIDO("EXCLUSÃO DE CLIENTE"),
    USUARIO_CRIADO("INCLUSÃO DE USUÁRIO"),
    USUARIO_ALTERADO("ALTERAÇÃO DE USUÁRIO"),
    USUARIO_REMOVIDO("EXCLUSÃO DE USUÁRIO"),
    LOGIN("LOGIN NO SISTEMA"),
    LOGOUT("LOGOUT DO SISTEMA");

    private final String rotulo;

    TipoEvento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
