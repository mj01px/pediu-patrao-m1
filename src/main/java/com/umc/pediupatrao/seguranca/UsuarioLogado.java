package com.umc.pediupatrao.seguranca;

import com.umc.pediupatrao.entity.Carimbo;
import com.umc.pediupatrao.entity.Perfil;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Responde "quem esta fazendo isto e quando". O responsavel sai sempre do
 * contexto do Spring Security, nunca de um campo enviado na requisicao.
 */
@Component
public class UsuarioLogado {

    private final Clock relogio;

    public UsuarioLogado(Clock relogio) {
        this.relogio = relogio;
    }

    public Operador operador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Nenhum usuário autenticado.");
        }
        Perfil perfil = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replaceFirst("^ROLE_", ""))
                .filter(nome -> Arrays.stream(Perfil.values()).anyMatch(p -> p.name().equals(nome)))
                .map(Perfil::valueOf)
                .findFirst()
                .orElse(null);
        return new Operador(auth.getName(), perfil);
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    public Carimbo carimbo() {
        return new Carimbo(operador().login(), agora());
    }
}
