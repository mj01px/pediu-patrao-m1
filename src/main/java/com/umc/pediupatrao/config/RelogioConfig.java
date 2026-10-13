package com.umc.pediupatrao.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relogio unico do sistema. Entrada, saida e auditoria usam o fuso da pizzaria,
 * mesmo que o servidor esteja configurado em UTC.
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio(@Value("${pediupatrao.fuso-horario}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }
}
