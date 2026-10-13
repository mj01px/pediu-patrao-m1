package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Mudanca;
import com.umc.pediupatrao.entity.TipoEvento;
import java.util.List;

/**
 * Evento publicado pelos services sempre que algo auditavel acontece.
 * Quem escuta e grava e o AuditoriaService: os services nao conhecem a auditoria.
 */
public record RegistroAlteracao(TipoEvento tipo, String entidade, String registroId,
        List<Mudanca> mudancas, String justificativa) {

    public static RegistroAlteracao de(TipoEvento tipo, String entidade, String registroId, Mudancas mudancas) {
        return new RegistroAlteracao(tipo, entidade, registroId, mudancas.lista(), null);
    }

    public RegistroAlteracao comJustificativa(String texto) {
        return new RegistroAlteracao(tipo, entidade, registroId, mudancas, texto);
    }
}
