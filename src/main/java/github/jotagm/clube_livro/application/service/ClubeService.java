package github.jotagm.clube_livro.application.service;

import github.jotagm.clube_livro.adapter.out.persistence.ClubeRepository;
import github.jotagm.clube_livro.domain.clube.Clube;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ClubeService {

    private final ClubeRepository clubeRepository;

    public Clube salvar(Clube clube) {
        return clubeRepository.save(clube);
    }

    public Clube buscarPorId(UUID id) {
        return clubeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clube não encontrado"));
    }

    public Clube buscarPorNome(String nome) {
        return clubeRepository.findByNome(nome)
                .orElseThrow(() -> new RuntimeException("Clube não encontrado"));
    }

    public List<Clube> listarTodos() {
        return clubeRepository.findAll();
    }

    public Clube atualizar(Clube clube) {
        return clubeRepository.save(clube);
    }

    public void deletar(UUID id) {
        clubeRepository.deleteById(id);
    }
}
