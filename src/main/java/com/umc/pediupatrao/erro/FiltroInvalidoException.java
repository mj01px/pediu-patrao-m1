package com.umc.pediupatrao.erro;

/** Parametro de consulta do dashboard fora do permitido (periodo invertido, longo demais...). Responde 400. */
public class FiltroInvalidoException extends RuntimeException {

    private final String parametro;

    public FiltroInvalidoException(String parametro, String mensagem) {
        super(mensagem);
        this.parametro = parametro;
    }

    public String getParametro() {
        return parametro;
    }
}
