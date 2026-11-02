package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.DiaDeLeituraRepository;
import github.jotagm.clube_livro.domain.usuario.DiaDeLeitura;
import github.jotagm.clube_livro.support.RelogioDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequenciaDeLeituraServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    // Quarta-feira, 4 de novembro de 2026 (a semana começou no domingo, dia 1º).
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 4);

    @Mock private DiaDeLeituraRepository repository;
    @Spy private RelogioDeTeste relogio = new RelogioDeTeste(SP);

    @InjectMocks private SequenciaDeLeituraService service;

    @BeforeEach
    void fixarHoje() {
        relogio.fixarEm(HOJE.atTime(10, 0).atZone(SP).toInstant());
    }

    private void diasLidos(LocalDate... dias) {
        when(repository.diasDesde(eq(7), any())).thenReturn(List.of(dias));
    }

    @Test
    void semLeituraNenhumaEZero() {
        diasLidos();
        assertThat(service.semanasSeguidas(7)).isZero();
    }

    @Test
    void contaSemanasSeguidasIncluindoAAtual() {
        // Esta semana (seg 2), a anterior (sáb 31/out) e a outra (dom 18/out é de outra semana: 18–24).
        diasLidos(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 30),
                LocalDate.of(2026, 10, 19));
        assertThat(service.semanasSeguidas(7)).isEqualTo(3);
    }

    @Test
    void semanaAtualSemLeituraAindaNaoQuebraASequencia() {
        diasLidos(LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 20));
        assertThat(service.semanasSeguidas(7)).isEqualTo(2);
    }

    @Test
    void buracoDeUmaSemanaQuebraASequencia() {
        // Leu esta semana e há três semanas; a semana passada ficou vazia.
        diasLidos(HOJE, LocalDate.of(2026, 10, 14));
        assertThat(service.semanasSeguidas(7)).isEqualTo(1);
    }

    @Test
    void domingoAbreASemanaESabadoFecha() {
        assertThat(SequenciaDeLeituraService.inicioDaSemana(LocalDate.of(2026, 11, 1)))
                .isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(SequenciaDeLeituraService.inicioDaSemana(LocalDate.of(2026, 11, 7)))
                .isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void registrarHojeGravaUmaVezPorDiaNoFusoDoApp() {
        // 23h30 em São Paulo já é dia seguinte em UTC: o dia gravado é o de São Paulo.
        relogio.fixarEm(HOJE.atTime(23, 30).atZone(SP).toInstant());
        assertThat(HOJE.atTime(23, 30).atZone(SP).withZoneSameInstant(ZoneOffset.UTC).toLocalDate())
                .isEqualTo(HOJE.plusDays(1));

        when(repository.existsByUsuarioIdAndDia(7, HOJE)).thenReturn(false);
        service.registrarHoje(7);
        verify(repository).save(any(DiaDeLeitura.class));

        when(repository.existsByUsuarioIdAndDia(7, HOJE)).thenReturn(true);
        service.registrarHoje(7);
        verify(repository, org.mockito.Mockito.times(1)).save(any(DiaDeLeitura.class));
    }

    @Test
    void falhaAoMarcarODiaNuncaDerrubaOProgresso() {
        when(repository.existsByUsuarioIdAndDia(7, HOJE)).thenReturn(false);
        when(repository.save(any(DiaDeLeitura.class))).thenThrow(new DataIntegrityViolationException("duplicado"));
        assertThatCode(() -> service.registrarHoje(7)).doesNotThrowAnyException();

        service.registrarHoje(null);
        verify(repository, never()).existsByUsuarioIdAndDia(eq(null), any());
    }
}
