package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Linha do pedido. Nome e preco sao uma "foto" do produto no momento da venda:
 * reajustar o cardapio depois nao muda pedidos antigos.
 */
public class ItemPedido {

    private String produtoId;
    private String descricao;
    private BigDecimal precoUnitario;
    private int quantidade;

    protected ItemPedido() {
        // usado pelo Spring Data ao ler do MongoDB
    }

    public ItemPedido(Produto produto, int quantidade) {
        this.produtoId = produto.getId();
        this.descricao = produto.getNome();
        this.precoUnitario = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
        this.quantidade = quantidade;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public String getProdutoId() {
        return produtoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }

    @Override
    public String toString() {
        return quantidade + "x " + descricao;
    }
}
