package com.umc.pediupatrao.entity;

/** Perfis de acesso. O nome vira a role do Spring Security (GERENTE vira ROLE_GERENTE). */
public enum Perfil {
    ADMIN("Administrador", "danger"),
    GERENTE("Gerente", "primary"),
    ATENDENTE("Atendente", "secondary");

    private final String rotulo;
    private final String cor; // cor do badge Bootstrap nas telas

    Perfil(String rotulo, String cor) {
        this.rotulo = rotulo;
        this.cor = cor;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getCor() {
        return cor;
    }
}
