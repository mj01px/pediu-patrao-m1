package com.umc.pediupatrao.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.TipoEvento;
import com.umc.pediupatrao.repository.EventoAuditoriaRepository;
import com.umc.pediupatrao.service.AuditoriaService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

/** Login e logout pela tela viram eventos da trilha, com usuario, perfil e horario. */
class RegistroDeAcessosTest {

    private final Clock relogio = Clock.fixed(Instant.parse("2026-10-16T17:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final EventoAuditoriaRepository repositorio = mock(EventoAuditoriaRepository.class);
    private final UsuarioLogado usuarioLogado = new UsuarioLogado(relogio);
    private final RegistroDeAcessos registro =
            new RegistroDeAcessos(new AuditoriaService(repositorio, usuarioLogado), usuarioLogado);

    @Test
    void loginGravaUsuarioPerfilEHorario() {
        registro.aoEntrar(new InteractiveAuthenticationSuccessEvent(gerente(), getClass()));

        EventoAuditoria evento = gravado();
        assertThat(evento.getTipo()).isEqualTo(TipoEvento.LOGIN);
        assertThat(evento.getUsuario()).isEqualTo("gerente");
        assertThat(evento.getPerfil()).isEqualTo(Perfil.GERENTE);
        assertThat(evento.getEntidade()).isEqualTo(AuditoriaService.ENTIDADE_ACESSO);
        assertThat(evento.getMomento()).isEqualTo(LocalDateTime.of(2026, 10, 16, 14, 30));
    }

    @Test
    void logoutUsaOUsuarioDoEventoMesmoSemContexto() {
        registro.aoSair(new LogoutSuccessEvent(gerente()));

        EventoAuditoria evento = gravado();
        assertThat(evento.getTipo()).isEqualTo(TipoEvento.LOGOUT);
        assertThat(evento.getUsuario()).isEqualTo("gerente");
        assertThat(evento.getMudancas()).isEmpty();
    }

    private EventoAuditoria gravado() {
        ArgumentCaptor<EventoAuditoria> captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(repositorio).save(captor.capture());
        return captor.getValue();
    }

    private static Authentication gerente() {
        return UsernamePasswordAuthenticationToken.authenticated("gerente", null,
                AuthorityUtils.createAuthorityList("ROLE_GERENTE"));
    }
}
