package com.umc.pediupatrao.seguranca;

import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.service.AuditoriaService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Leva o login e o logout pela tela para a trilha de auditoria, com quem e quando.
 * Chamadas a API com HTTP Basic nao entram: cada requisicao e uma autenticacao e lotaria a trilha.
 * A senha nunca passa por aqui, so o login e o perfil.
 */
@Component
public class RegistroDeAcessos {

    private final AuditoriaService auditoria;
    private final UsuarioLogado usuarioLogado;

    public RegistroDeAcessos(AuditoriaService auditoria, UsuarioLogado usuarioLogado) {
        this.auditoria = auditoria;
        this.usuarioLogado = usuarioLogado;
    }

    @EventListener
    public void aoEntrar(InteractiveAuthenticationSuccessEvent evento) {
        auditoria.registrarAcesso(TipoEvento.LOGIN, usuarioLogado.operadorDe(evento.getAuthentication()));
    }

    @EventListener
    public void aoSair(LogoutSuccessEvent evento) {
        auditoria.registrarAcesso(TipoEvento.LOGOUT, usuarioLogado.operadorDe(evento.getAuthentication()));
    }
}
