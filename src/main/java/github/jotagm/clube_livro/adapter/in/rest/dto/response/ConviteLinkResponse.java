package github.jotagm.clube_livro.adapter.in.rest.dto.response;

/** Link de convite do clube: {@code url} já pronto para compartilhar, {@code codigo} para os clientes montarem o próprio. */
public record ConviteLinkResponse(String codigo, String url) {}
