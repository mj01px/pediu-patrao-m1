package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;

/** Usuario como sai da API: nunca com a senha. */
public record UsuarioDTO(String id, String username, Perfil perfil) {

    public static UsuarioDTO de(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getUsername(), usuario.getPerfil());
    }
}
