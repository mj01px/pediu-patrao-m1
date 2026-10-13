package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoDTO(
        @NotBlank(message = "o motivo é obrigatório")
        @Size(min = 10, max = 500, message = "entre 10 e 500 caracteres") String motivo) {
}
