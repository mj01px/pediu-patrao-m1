package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada de criacao/edicao de usuario (tela e API). E classe, e nao record,
 * porque o th:field do Thymeleaf precisa de getters e setters.
 * Sem campo id: ninguem sobrescreve outro usuario mandando um id no corpo.
 */
public class UsuarioFormDTO {

    @NotBlank(message = "informe o login")
    @Size(min = 3, max = 40, message = "entre 3 e 40 caracteres")
    @Pattern(regexp = "[a-z0-9._-]+", message = "use letras minúsculas, números, ponto, hífen ou underline")
    private String username;

    // Obrigatoria so ao criar (regra no service). 72 e o limite do BCrypt.
    @Size(max = 72, message = "senha longa demais")
    private String senha;

    @NotNull(message = "escolha um perfil")
    private Perfil perfil;

    public static UsuarioFormDTO de(Usuario usuario) {
        UsuarioFormDTO form = new UsuarioFormDTO();
        form.username = usuario.getUsername();
        form.perfil = usuario.getPerfil();
        return form;
    }

    public boolean informouSenha() {
        return senha != null && !senha.isBlank();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }
}
