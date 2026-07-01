package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.TemaRepository;
import github.jotagm.clube_livro.domain.tema.Tema;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TemaService {

    private final TemaRepository temaRepository;

    public Tema salvar(Tema tema) {
        return temaRepository.save(tema);
    }

    public Tema buscarPorId(UUID id) {
        return temaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tema não encontrado"));
    }

    public Tema buscarPorNome(String nome) {
        return temaRepository.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Tema não encontrado"));
    }

    public List<Tema> listarTodos() {
        return temaRepository.findAll();
    }

    public Tema atualizar(Tema tema) {
        return temaRepository.save(tema);
    }

    public void deletar(UUID id) {
        temaRepository.deleteById(id);
    }
}
