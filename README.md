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
Monólito com DDD e arquitetura hexagonal (ports and adapters). Os services da camada de
aplicação só falam com a infraestrutura por ports (`PublicadorDeEventos`,
`ArmazenamentoDeFotos`, `CatalogoDeLivros`, `EnviadorDeConvite`, `RegistroDeFalhasDeLogin`).
Controllers nunca expõem entidades, sempre DTOs, e os services não conhecem HTTP: lançam
exceções de domínio que o `GlobalExceptionHandler` traduz em status.

**Uma concessão consciente:** as entidades do domínio carregam as anotações do JPA
(`@Entity`, `@Column`). Separar exigiria um modelo de persistência espelhado e conversores
para cada uma das 14 entidades, sem ganho real com um único banco relacional. O que é
infraestrutura de verdade (outbox, desafios de OTP, falhas de login, eventos processados) tem
entidade própria nos adapters, fora do domínio.

O schema é versionado com Flyway (`src/main/resources/db/migration`); o Hibernate só confere
se as entidades batem com ele (`ddl-auto: validate`).
```
src/main/java/github/jotagm/clube_livro/
├── domain/            # entidades e regras: clube, convite, leitura, votacao, estante, feed...
├── application/
│   ├── port/out/      # o que o domínio precisa do mundo de fora
│   └── service/
├── adapter/
│   ├── in/            # rest, scheduler, messaging (consumidor SQS)
│   └── out/           # persistence, outbox (SNS), storage (S3), email, client (Google Books)
└── configs/           # segurança, JWT, fuso, clientes AWS
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
| Migrações | Flyway |
| Banco de dados | PostgreSQL 16 |
| Infra | AWS (EC2 + Docker Compose + Caddy, RDS, SNS/SQS, S3) via Terraform |
| CI/CD | GitHub Actions (OIDC, ECR, SSM) |
| E-mail | Client_GO (serviço em Go) + Resend |
| Integração externa | Google Books API, Open Library (reserva) |
| Documentação da API | springdoc-openapi (Swagger UI) |

---
## Documentação da API
Com a aplicação no ar, a especificação OpenAPI é gerada a partir do próprio código:

| Recurso | Caminho |
|---|---|
| Swagger UI | `/swagger-ui.html` |
| OpenAPI JSON | `/v3/api-docs` |

Ambos são públicos. Para testar os endpoints protegidos: crie um usuário em `POST /usuarios`,
autentique em `POST /auth/login` e informe o token retornado no botão **Authorize**.
