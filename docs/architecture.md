# Arquitetura e convenções

## Contexto

Frontend Next.js e backend Spring Boot vivem em repositórios separados e conversam por API REST. O backend é um monólito modular com PostgreSQL e Flyway. Microserviços, Redis, filas e Kubernetes ficam fora do MVP.

## Regras

- Controller valida transporte e delega; não concentra regra de negócio.
- Application services coordenam casos de uso e transações.
- Entidades e repositórios permanecem no módulo de domínio.
- DTOs nunca expõem hash de senha ou entidades JPA diretamente.
- Datas são armazenadas em UTC.
- Alterações de schema exigem nova migration Flyway; migrations aplicadas nunca são editadas.
- Erros usam `timestamp`, `status`, `code`, `message`, `path` e `fields`.
- A API pública é versionada sob `/api/v1`.

## Definition of Done

Critérios atendidos, PR revisado, testes atualizados, build verde, Swagger e README atualizados quando necessário, migration incluída quando aplicável e nenhum segredo versionado.
