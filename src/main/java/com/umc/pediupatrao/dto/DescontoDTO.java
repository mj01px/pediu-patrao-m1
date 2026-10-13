package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DescontoDTO(
        @NotNull(message = "informe o percentual")
        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        @DecimalMax(value = "20.00", message = "o limite é 20%") BigDecimal percentual) {
}
