package com.umc.pediupatrao.service.dashboard;

import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.erro.FiltroInvalidoException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Filtros comuns a todos os endpoints do dashboard (RF05).
 *
 * O periodo e fechado nas duas pontas em dias (inicio e fim entram) e vira
 * [inicio 00:00, fim+1 00:00) na consulta. Sem datas, vale os ultimos 30 dias.
 */
public record FiltroDashboard(LocalDate inicio, LocalDate fim, String tipo, StatusPedido status) {

    public static final int DIAS_PADRAO = 30;
    public static final int LIMITE_DIAS = 366;

    public static FiltroDashboard de(LocalDate inicio, LocalDate fim, String tipo, StatusPedido status, LocalDate hoje) {
        LocalDate ate = fim != null ? fim : hoje;
        LocalDate de = inicio != null ? inicio : ate.minusDays(DIAS_PADRAO - 1L);
        if (de.isAfter(ate)) {
            throw new FiltroInvalidoException("inicio", "A data inicial (" + de + ") é posterior à final (" + ate + ").");
        }
        if (ChronoUnit.DAYS.between(de, ate) + 1 > LIMITE_DIAS) {
            throw new FiltroInvalidoException("fim", "O período pode ter no máximo " + LIMITE_DIAS + " dias.");
        }
        String categoria = tipo == null || tipo.isBlank() ? null : tipo.trim();
        if (categoria != null && categoria.length() > 40) {
            throw new FiltroInvalidoException("tipo", "Categoria com mais de 40 caracteres.");
        }
        return new FiltroDashboard(de, ate, categoria, status);
    }

    public long dias() {
        return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }

    public LocalDateTime desde() {
        return inicio.atStartOfDay();
    }

    /** Exclusivo: o primeiro instante depois do periodo. */
    public LocalDateTime ate() {
        return fim.plusDays(1).atStartOfDay();
    }

    /** Mesmo tamanho, logo antes: base de comparacao do resumo. */
    public FiltroDashboard periodoAnterior() {
        long dias = dias();
        return new FiltroDashboard(inicio.minusDays(dias), inicio.minusDays(1), tipo, status);
    }

    public boolean aceitaTipo(String tipoDoProduto) {
        return tipo == null || tipo.equalsIgnoreCase(tipoDoProduto);
    }
}
