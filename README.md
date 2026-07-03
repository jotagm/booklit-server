# Booklit
Grupos de leitura vivem no WhatsApp hoje. Funciona para conversa — mas não oferece estrutura para definir metas de leitura, acompanhar o progresso de cada membro ou votar no próximo livro de forma organizada.
Booklit é uma alternativa estruturada: um app mobile para clubes de leitura que transforma o caos do grupo de WhatsApp em uma experiência gerenciada, com ritmo definido pelo líder e visibilidade individual de progresso.
---
## O que diferencia do WhatsApp
- Líder define metas de leitura por páginas ou capítulos
- Cada membro registra e visualiza seu próprio progresso
- Votação nativa para o próximo livro — voto do líder vale 2
- Histórico organizado por clube e por livro
- Discussões contextualizadas por leitura, não por data
---
## Uma decisão de design que vale destacar
Leituras têm início e fim. Uma abordagem óbvia seria persistir um campo `status` (`ATIVA`, `ENCERRADA`) e atualizá-lo manualmente ou via job agendado.
Optei por não fazer isso. Uma leitura está ativa quando `data_inicio <= NOW() <= data_fim` — o status é derivado do tempo, não armazenado. Isso elimina uma classe inteira de bugs de inconsistência entre o estado real e o estado salvo, e simplifica a lógica de negócio.
---
## Arquitetura
Domain-Driven Design com Layered Architecture. O domínio é completamente isolado de infraestrutura — entidades de domínio não conhecem JPA. Controllers nunca expõem entidades diretamente, sempre DTOs.
```
src/main/java/github/jotagm/booklit/
├── domain/
│   ├── usuario/
│   └── clube/
│       ├── convite/
│       ├── leitura/
│       └── votacao/
├── adapter/
│   ├── in/
│   │   └── rest/
│   └── out/
│       └── persistence/
```
```
Usuario ──── UsuarioClube ──── Clube
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
                 Convite    LeituraClube   Votacao
                                 │            │
                             Registro     OpcaoVoto
                                              │
                                            Voto
```
---
## Stack
| Camada | Tecnologia |
|---|---|
| Mobile | Flutter (Dart) |
| Backend | Spring Boot 3.4.5 + Java 21 |
| Autenticação | Spring Security + JWT |
| ORM | Spring Data JPA + Hibernate |
| Banco de dados | PostgreSQL 16 |
| Deploy | Railway + Neon |
| Integração externa | Google Books API |
#   b o o k l i t - s e r v e r 
 
 #   b o o k l i t - s e r v e r 
 
 
