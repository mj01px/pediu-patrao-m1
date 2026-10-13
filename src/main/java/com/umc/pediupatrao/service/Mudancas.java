package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Mudanca;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Acumula os campos que mudaram (de -> para), ignorando os que ficaram iguais. */
public final class Mudancas {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final List<Mudanca> itens = new ArrayList<>();

    private Mudancas() {
    }

    public static Mudancas nova() {
        return new Mudancas();
    }

    public Mudancas registrar(String campo, Object de, Object para) {
        String antes = comoTexto(de);
        String depois = comoTexto(para);
        if (!Objects.equals(antes, depois)) {
            itens.add(new Mudanca(campo, antes, depois));
        }
        return this;
    }

    /** Compara o valor real, mas guarda mascarado: a trilha mostra que mudou sem expor o dado. */
    public Mudancas registrarMascarado(String campo, String de, String para) {
        if (!Objects.equals(comoTexto(de), comoTexto(para))) {
            itens.add(new Mudanca(campo, mascarar(de), mascarar(para)));
        }
        return this;
    }

    public boolean vazia() {
        return itens.isEmpty();
    }

    public List<Mudanca> lista() {
        return List.copyOf(itens);
    }

    /** (11) 98765-4321 vira ******4321. */
    static String mascarar(String valor) {
        String texto = comoTexto(valor);
        if (texto == null) {
            return null;
        }
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() <= 4) {
            return "****";
        }
        return "*".repeat(digitos.length() - 4) + digitos.substring(digitos.length() - 4);
    }

    private static String comoTexto(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal numero) {
            return numero.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (valor instanceof LocalDateTime momento) {
            return momento.format(DATA_HORA);
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }
}
