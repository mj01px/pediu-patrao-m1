package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.repository.EventoAuditoriaRepository;
import com.umc.pediupatrao.seguranca.Operador;
import com.umc.pediupatrao.seguranca.RegrasAcesso;
import com.umc.pediupatrao.seguranca.UsuarioLogado;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final EventoAuditoriaRepository repositorio;
    private final UsuarioLogado usuarioLogado;

    public AuditoriaService(EventoAuditoriaRepository repositorio, UsuarioLogado usuarioLogado) {
        this.repositorio = repositorio;
        this.usuarioLogado = usuarioLogado;
    }

    /**
     * Escuta os RegistroAlteracao publicados pelos services e grava na trilha.
     * Roda na mesma thread da operacao, entao enxerga o usuario logado.
     */
    @EventListener
    public void gravar(RegistroAlteracao alteracao) {
        Operador operador = usuarioLogado.operador();
        repositorio.save(new EventoAuditoria(usuarioLogado.agora(), operador.login(), operador.perfil(),
                alteracao.tipo(), alteracao.entidade(), alteracao.registroId(),
                alteracao.mudancas(), alteracao.justificativa()));
        // So metadados no log: nada de valores, senhas ou dados pessoais
        log.info("auditoria tipo={} entidade={} id={} usuario={}",
                alteracao.tipo(), alteracao.entidade(), alteracao.registroId(), operador.login());
    }

    @PreAuthorize(RegrasAcesso.SUPERVISAO)
    public List<EventoAuditoria> pesquisar(String entidade, String registroId) {
        if (StringUtils.hasText(entidade) && StringUtils.hasText(registroId)) {
            return repositorio.findByEntidadeAndRegistroIdOrderByMomentoDesc(entidade, registroId.trim());
        }
        if (StringUtils.hasText(entidade)) {
            return repositorio.findTop300ByEntidadeOrderByMomentoDesc(entidade);
        }
        return repositorio.findTop300ByOrderByMomentoDesc();
    }
}
