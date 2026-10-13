package com.umc.pediupatrao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Tudo o que a API aceita para abrir um pedido. Nao ha preco, total nem status:
 * se a requisicao mandar "total", o campo e ignorado.
 */
public record NovoPedidoDTO(
        @NotBlank(message = "escolha o cliente") String clienteId,
        @NotEmpty(message = "inclua ao menos um item")
        @Size(max = 30, message = "no máximo 30 itens") List<@Valid ItemDTO> itens,
        @Size(max = 300, message = "no máximo 300 caracteres") String observacao) {

    /** Um item: so o produto e a quantidade. O preco vem do cardapio. */
    public record ItemDTO(
            @NotBlank(message = "escolha o produto") String produtoId,
            @Min(value = 1, message = "mínimo 1") @Max(value = 99, message = "máximo 99") int quantidade) {
    }
}
