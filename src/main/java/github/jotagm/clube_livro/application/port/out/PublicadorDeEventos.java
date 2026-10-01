package github.jotagm.clube_livro.application.port.out;

import github.jotagm.clube_livro.domain.evento.EventoDeDominio;

public interface PublicadorDeEventos {
    void publicar(EventoDeDominio evento);
}