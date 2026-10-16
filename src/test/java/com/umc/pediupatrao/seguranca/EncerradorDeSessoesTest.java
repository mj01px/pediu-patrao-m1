package com.umc.pediupatrao.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/** Mudou o acesso de alguem: so as sessoes daquele login caem, as dos outros continuam. */
class EncerradorDeSessoesTest {

    private final SessionRegistry registro = new SessionRegistryImpl();
    private final EncerradorDeSessoes encerrador = new EncerradorDeSessoes(registro);

    @Test
    void encerraTodasAsSessoesDoLoginEPreservaAsDosOutros() {
        registro.registerNewSession("s1", usuario("atendente"));
        registro.registerNewSession("s2", usuario("atendente"));
        registro.registerNewSession("s3", usuario("gerente"));

        encerrador.encerrar("atendente");

        assertThat(registro.getSessionInformation("s1").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("s2").isExpired()).isTrue();
        assertThat(registro.getSessionInformation("s3").isExpired()).isFalse();
    }

    @Test
    void loginSemSessaoAbertaNaoFazNada() {
        registro.registerNewSession("s1", usuario("gerente"));

        encerrador.encerrar("ninguem");

        assertThat(registro.getSessionInformation("s1").isExpired()).isFalse();
    }

    private static UserDetails usuario(String login) {
        return User.withUsername(login).password("x").roles("ATENDENTE").build();
    }
}
