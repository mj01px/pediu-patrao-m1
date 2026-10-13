package com.umc.pediupatrao.entity;

/** Um campo que mudou: "logradouro: Rua das Flores -> Av. Brasil". */
public class Mudanca {

    private String campo;
    private String de;
    private String para;

    protected Mudanca() {
        // usado pelo Spring Data ao ler do MongoDB
    }

    public Mudanca(String campo, String de, String para) {
        this.campo = campo;
        this.de = de;
        this.para = para;
    }

    public String getCampo() {
        return campo;
    }

    public String getDe() {
        return de;
    }

    public String getPara() {
        return para;
    }
}
