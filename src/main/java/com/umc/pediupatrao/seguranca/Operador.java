package com.umc.pediupatrao.seguranca;

import com.umc.pediupatrao.entity.Perfil;

/** O funcionario logado que esta executando a operacao. */
public record Operador(String login, Perfil perfil) {
}
