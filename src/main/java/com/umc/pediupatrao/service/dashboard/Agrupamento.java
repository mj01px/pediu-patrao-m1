package com.umc.pediupatrao.service.dashboard;

/** Como a serie de vendas e fatiada no tempo (GET /api/dashboard/vendas?agrupamento=). */
public enum Agrupamento {
    DIA("Por dia"),
    SEMANA("Por semana"),
    MES("Por mês"),
    HORA("Por hora do dia");

    private final String rotulo;

    Agrupamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
