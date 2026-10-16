package com.umc.pediupatrao.entity;

/** Perfis de acesso. O nome vira a role do Spring Security (GERENTE vira ROLE_GERENTE). */
public enum Perfil {
    ADMIN("Administrador"),
    GERENTE("Gerente"),
    ATENDENTE("Atendente");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
