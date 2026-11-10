package com.umc.pediupatrao.service.dashboard;

/** Criterio do ranking de produtos (GET /api/dashboard/produtos/mais-vendidos?ordenarPor=). */
public enum OrdemRanking {
    QUANTIDADE("Quantidade vendida"),
    FATURAMENTO("Faturamento");

    private final String rotulo;

    OrdemRanking(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
