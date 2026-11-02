package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.DiaDeLeituraRepository;
import github.jotagm.clube_livro.domain.usuario.DiaDeLeitura;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

/**
 * "🔥 3 semanas": quantas semanas seguidas a pessoa registrou leitura. A semana começa no
 * domingo, como no calendário do app. Uma semana atual ainda sem leitura não quebra a sequência,
 * que passa a contar a partir da anterior (a pessoa ainda tem até sábado).
 */
@Slf4j
@Service
@AllArgsConstructor
public class SequenciaDeLeituraService {

    /** Teto da conta: evita varrer anos de histórico para um número que ninguém lê. */
    static final int MAXIMO_DE_SEMANAS = 104;

    private final DiaDeLeituraRepository repository;
    private final Clock relogio;

    /**
     * Marca hoje como dia de leitura. Roda numa transação própria e nunca lança: é um efeito
     * colateral do progresso, que não pode falhar por causa dele.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarHoje(Integer usuarioId) {
        if (usuarioId == null) return;
        LocalDate hoje = LocalDate.now(relogio);
        try {
            if (!repository.existsByUsuarioIdAndDia(usuarioId, hoje)) {
                repository.save(DiaDeLeitura.de(usuarioId, hoje));
            }
        } catch (DataIntegrityViolationException e) {
            // Duas gravações no mesmo segundo: a outra já marcou o dia.
        } catch (RuntimeException e) {
            log.warn("Falha ao marcar dia de leitura usuario={}: {}", usuarioId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public int semanasSeguidas(Integer usuarioId) {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate semanaAtual = inicioDaSemana(hoje);
        LocalDate desde = semanaAtual.minusWeeks(MAXIMO_DE_SEMANAS);

        Set<LocalDate> semanas = new HashSet<>();
        for (LocalDate dia : repository.diasDesde(usuarioId, desde)) {
            semanas.add(inicioDaSemana(dia));
        }

        LocalDate cursor = semanas.contains(semanaAtual) ? semanaAtual : semanaAtual.minusWeeks(1);
        int total = 0;
        while (semanas.contains(cursor) && total < MAXIMO_DE_SEMANAS) {
            total++;
            cursor = cursor.minusWeeks(1);
        }
        return total;
    }

    @Transactional(readOnly = true)
    public boolean leuHoje(Integer usuarioId) {
        return repository.existsByUsuarioIdAndDia(usuarioId, LocalDate.now(relogio));
    }

    static LocalDate inicioDaSemana(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }
}
