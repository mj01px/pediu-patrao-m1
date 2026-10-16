package com.umc.pediupatrao.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Derruba as sessoes abertas de um login. Sem isto, quem perde um perfil continuaria com as
 * permissoes antigas ate sair do sistema, porque a sessao guarda as roles do momento do login.
 * A sessao marcada como expirada e invalidada no proximo acesso (ConcurrentSessionFilter).
 */
@Component
public class EncerradorDeSessoes {

    private static final Logger log = LoggerFactory.getLogger(EncerradorDeSessoes.class);

    private final SessionRegistry sessoes;

    public EncerradorDeSessoes(SessionRegistry sessoes) {
        this.sessoes = sessoes;
    }

    public void encerrar(String login) {
        int total = 0;
        for (Object principal : sessoes.getAllPrincipals()) {
            if (principal instanceof UserDetails usuario && usuario.getUsername().equals(login)) {
                for (SessionInformation sessao : sessoes.getAllSessions(principal, false)) {
                    sessao.expireNow();
                    total++;
                }
            }
        }
        if (total > 0) {
            log.info("Sessões encerradas: usuario={} quantidade={}", login, total);
        }
    }
}
