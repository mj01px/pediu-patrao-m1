package com.umc.pediupatrao.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Formatos usados pelos templates, chamados como ${@exibir.moeda(pedido.total)}.
 * Fica aqui para as telas mostrarem tudo do mesmo jeito.
 */
@Component("exibir")
public class Exibir {

    private static final Locale BRASIL = Locale.of("pt", "BR");

    /** Codigo curto do pedido: os 4 ultimos caracteres do id. Ex.: "#6A1F". */
    public String codigo(String id) {
        if (id == null || id.isBlank()) {
            return "#----";
        }
        return "#" + id.substring(Math.max(0, id.length() - 4)).toUpperCase(Locale.ROOT);
    }

    /** Ex.: 112.8 vira "R$ 112,80". */
    public String moeda(Number valor) {
        if (valor == null) {
            return "R$ 0,00";
        }
        NumberFormat formato = NumberFormat.getCurrencyInstance(BRASIL);
        // O formato do Java usa espaco nao separavel; o comum basta para a tela
        return formato.format(valor).replace(' ', ' ');
    }

    /** Ex.: 10.00 vira "10%" e 12.5 vira "12,5%". */
    public String percentual(BigDecimal valor) {
        if (valor == null) {
            return "0%";
        }
        NumberFormat formato = NumberFormat.getNumberInstance(BRASIL);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor) + "%";
    }
}
