package com.umc.pediupatrao.erro;

/** Regra de negocio violada (transicao invalida, desconto acima do limite...). Responde 422. */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
