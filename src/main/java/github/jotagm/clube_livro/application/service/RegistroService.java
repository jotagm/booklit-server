package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.RegistroRepository;
import github.jotagm.clube_livro.domain.clube.leitura.Registro;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RegistroService {

    private final RegistroRepository registroRepository;

    public Registro salvar(Registro registro) {
        return registroRepository.save(registro);
    }

    public Registro buscarPorId(UUID id) {
        return registroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado"));
    }

    public List<Registro> listarPorLeitura(UUID leituraClubeId) {
        return registroRepository.findByLeituraClubeId(leituraClubeId);
    }

    public List<Registro> listarPorUsuario(UUID usuarioId) {
        return registroRepository.findByUsuarioId(usuarioId);
    }

    public Registro buscarPorLeituraEUsuario(UUID leituraClubeId, UUID usuarioId) {
        return registroRepository.findByLeituraClubeIdAndUsuarioId(leituraClubeId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado para este usuário nesta leitura"));
    }

    public Registro atualizarProgresso(UUID leituraId, UUID usuarioId, int novoValor) {
        Registro registro = registroRepository.findByLeituraClubeIdAndUsuarioId(leituraId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado"));

        if (novoValor < registro.getValorAtual()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "O progresso não pode diminuir");
        }

        if (novoValor > registro.getLeituraClube().getValorMeta()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "O progresso não pode ultrapassar a meta da leitura");
        }

        registro.setValorAtual(novoValor);
        registro.setUpdatedAt(LocalDateTime.now());
        return registroRepository.save(registro);
    }
}
