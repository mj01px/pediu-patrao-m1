package com.umc.pediupatrao.erro;

/** O registro procurado nao existe. Responde 404. */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String entidade, String id) {
        super(entidade + " não encontrado: " + id);
    }
}
