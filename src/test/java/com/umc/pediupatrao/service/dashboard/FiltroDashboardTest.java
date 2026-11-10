package com.umc.pediupatrao.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.umc.pediupatrao.erro.FiltroInvalidoException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FiltroDashboardTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @Test
    void semDatasUsaOsUltimos30Dias() {
        FiltroDashboard filtro = FiltroDashboard.de(null, null, " ", null, HOJE);

        assertThat(filtro.inicio()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(filtro.fim()).isEqualTo(HOJE);
        assertThat(filtro.dias()).isEqualTo(30);
        assertThat(filtro.tipo()).isNull(); // categoria em branco = todas
    }

    @Test
    void periodoVaiDaMeiaNoiteDoInicioAteAMeiaNoiteDepoisDoFim() {
        FiltroDashboard filtro = FiltroDashboard.de(HOJE, HOJE, null, null, HOJE);

        assertThat(filtro.desde()).isEqualTo(LocalDateTime.of(2026, 11, 10, 0, 0));
        assertThat(filtro.ate()).isEqualTo(LocalDateTime.of(2026, 11, 11, 0, 0));
    }

    @Test
    void periodoAnteriorTemOMesmoTamanhoEEncostaNoAtual() {
        FiltroDashboard anterior = FiltroDashboard.de(LocalDate.of(2026, 11, 1), HOJE, null, null, HOJE).periodoAnterior();

        assertThat(anterior.inicio()).isEqualTo(LocalDate.of(2026, 10, 22));
        assertThat(anterior.fim()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void recusaPeriodoInvertido() {
        assertThatThrownBy(() -> FiltroDashboard.de(HOJE, HOJE.minusDays(1), null, null, HOJE))
                .isInstanceOf(FiltroInvalidoException.class)
                .extracting("parametro").isEqualTo("inicio");
    }

    @Test
    void recusaPeriodoMaiorQueUmAno() {
        assertThatThrownBy(() -> FiltroDashboard.de(HOJE.minusDays(400), HOJE, null, null, HOJE))
                .isInstanceOf(FiltroInvalidoException.class)
                .extracting("parametro").isEqualTo("fim");
    }

    @Test
    void aceitaExatamente366Dias() {
        assertThat(FiltroDashboard.de(HOJE.minusDays(365), HOJE, null, null, HOJE).dias()).isEqualTo(366);
    }
}
