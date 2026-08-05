# Guia de implementação e entrega do backend

Atualizado em 2026-08-05.

## Objetivo

Este documento orienta a revisão e a entrega incremental do backend do Awaken. O
`main` contém oficialmente a fundação e a autenticação das Sprints 0 e 1. Há uma
implementação candidata das Sprints 2 a 13 no working tree local, mas esse código
não faz parte deste commit e não deve ser considerado entregue antes de revisão,
testes e commits próprios.

O backend é a fonte autoritativa para identidade, autorização, planos, sessões,
progressão, audiência social e recompensas. O frontend pode oferecer resposta
imediata e contingência offline, mas não deve recalcular regras de negócio.

## Como trabalhar com o estado local

1. Não execute `git add -A` enquanto os módulos candidatos estiverem misturados.
2. Preserve as alterações locais antes de trocar de branch ou reescrever commits.
3. Revise e entregue uma fatia vertical por vez: migration, domínio, serviço,
   controller, testes e documentação.
4. Use `git add` com caminhos explícitos para impedir que a próxima fatia seja
   incluída por acidente.
5. Não edite migrations Flyway já aplicadas. Qualquer correção de schema exige uma
   migration posterior.
6. Antes de cada commit, execute `mvn verify` e confira `git diff --cached`.

## Ordem recomendada de implementação

### 1. Sprint 2 — Perfil e onboarding

Base candidata: migration `V3__profile_and_onboarding.sql` e módulos `user`.

- Persistir modalidades, meta semanal, classe, universo e conclusão do onboarding.
- Manter histórico da meta semanal sem reescrever semanas anteriores.
- Permitir atualização de perfil somente pelo proprietário autenticado.
- Implementar exclusão de conta conforme a política de retenção aprovada.
- Garantir compatibilidade do `GET /api/v1/auth/me` com usuários antigos.

Endpoints esperados:

- `GET|PATCH /api/v1/users/me`
- `PATCH /api/v1/users/me/onboarding`
- `DELETE /api/v1/users/me`

### 2. Sprint 3 — Biblioteca de exercícios

Base candidata: migration `V4__exercise_catalog.sql` e módulo `exercise`.

- Separar exercícios globais de exercícios pessoais.
- Restringir administração global ao papel `ADMIN`.
- Aplicar busca e filtros sem diferença de acentos ou caixa.
- Impedir que um usuário consulte ou altere exercício pessoal de terceiro.

### 3. Sprint 4 — Planos versionados

Base candidata: migration `V5__versioned_workout_plans.sql` e módulo `plan`.

- Preservar ordem e parâmetros opcionais dos exercícios.
- Criar nova versão ao editar um plano usado anteriormente.
- Manter duplicação, arquivamento e restauração idempotentes.
- Nunca devolver plano pertencente a outro usuário.

### 4. Sprint 5 — Sessões de treino

Base candidata: migration `V6__workout_sessions.sql` e módulo `workout`.

- Permitir somente uma sessão `IN_PROGRESS` por usuário.
- Salvar snapshot imutável do plano e sua versão.
- Registrar séries, notas e estados `COMPLETED`, `SKIPPED` e `PENDING`.
- Tornar conclusão e cancelamento idempotentes.
- Permitir retomada sem duplicar sessão ou dados.

### 5. Sprint 6 — Mídia, feed e diário

Base candidata: migration `V7__media_feed_and_comments.sql` e módulos `feed` e
`MediaService`.

- Aceitar apenas JPG, PNG ou WebP de até 5 MB.
- Validar autoria, checksum e vínculo da foto com a sessão.
- Publicar somente mediante ação explícita do usuário.
- Manter foto opcional e card de contingência, conforme DEC-601.
- Considerar recordes pessoais válidos pelo registro do usuário, conforme DEC-602.
- Proteger autoria na criação e exclusão de comentários.

### 6. Sprint 7 — Progressão e conquistas

Base candidata: migration `V8__progression_and_universe_expansion.sql` e módulo
`progression`.

- Criar um único evento de XP por sessão concluída.
- Calcular nível, rank e constância exclusivamente no backend.
- Usar constância semanal, sem punir um dia isolado de descanso.
- Manter a curva de XP configurável e explicável.
- Tratar universos como apresentação: nunca alterar XP, regra ou permissão.

### 7. Sprint 8 — Amigos e grupos

Base candidata: migration `V9__friends_groups_and_invites.sql` e módulo `social`.

- Tornar solicitações e aceite de amizade idempotentes.
- Invalidar convites expirados, revogados ou já consumidos.
- Limitar grupos a 20 membros inclusive sob concorrência.
- Garantir que grupo privado, membros e feed respeitem vínculo ativo.
- Impedir que o único administrador abandone o grupo sem transferência.

### 8. Sprint 9 — Ranking e notificações

Base candidata: migration `V10__rankings_notifications_and_privacy.sql` e módulos
`ranking` e `notification`.

- Fechar períodos semanal e mensal antes de publicar posições.
- Aplicar desempate por dias distintos com check-in e depois check-ins brutos.
- Oferecer ranking global permanente e ranking restrito ao grupo.
- Evitar spam e permitir preferências e marcação idempotente de leitura.
- Não expor dados privados no ranking ou nas notificações.

### 9. Sprint 13 — Progressão avançada e prestígio

Base candidata: migrations `V11__advanced_progression.sql` e
`V12__monarch_and_hall_of_fame.sql`, além de `AdvancedProgressionService` e
`MonarchService`.

- Manter maestria e ledger independentes para cada classe.
- Evoluir de S-5 até S-1 por consistência semanal configurável.
- Pausar avaliação sem apagar o tempo já cumprido.
- Promover para Nacional após o período elegível configurado.
- Garantir um único Monarca ativo com bloqueio e restrição no banco.
- Preservar todos os reinados no Hall da Fama.

## Próximas APIs de backend planejadas

As Sprints 15 e 16 foram registradas no ClickUp e ainda não possuem implementação:

- persistir objetivo, experiência, duração e equipamentos do usuário;
- recomendar a Missão de hoje por regras determinísticas e explicáveis;
- oferecer descanso sem perda de XP;
- aceitar registro rápido idempotente de séries;
- detectar recordes pessoais com base nos dados informados pelo usuário.

IA generativa, GPS e orientação médica ficam fora desse primeiro corte. Uma
recomendação de treino é uma sugestão e precisa de limites claros de segurança.

## Invariantes obrigatórios

- Todos os recursos privados derivam o proprietário do token, nunca do payload.
- Recursos de terceiros retornam `404` quando isso evita enumeração indevida.
- Operações repetíveis usam `Idempotency-Key` ou uma restrição única equivalente.
- XP e recompensas possuem origem auditável e nunca são duplicados.
- Descanso, pausa ou troca de missão não removem XP conquistado.
- Datas são persistidas em UTC.
- Entidades JPA e hashes de senha nunca aparecem em respostas.
- Fotos permanecem privadas até publicação explícita.
- Logs não contêm token, senha, foto ou conteúdo pessoal.
- Erros seguem `timestamp`, `status`, `code`, `message`, `path` e `fields`.

## Estratégia de testes

Cada fatia deve conter testes de serviço e integração para:

- caminho feliz e validação de entrada;
- usuário não autenticado;
- tentativa de acesso a recurso de terceiro;
- concorrência e repetição da mesma requisição;
- limites de paginação, grupo e upload;
- migração em PostgreSQL e contexto de teste em H2;
- valores de fronteira de XP, datas e períodos.

Validação mínima:

```powershell
mvn verify
docker compose up -d
mvn spring-boot:run
```

Depois de iniciar a aplicação, validar:

- health: `GET /actuator/health`;
- Swagger: `/swagger-ui.html`;
- cadastro, login e `GET /api/v1/auth/me`;
- fluxo vertical correspondente à fatia em revisão.

## Modelo de commits

Preferir commits pequenos e revisáveis, por exemplo:

```text
feat: implement profile and onboarding
feat: add exercise catalog
feat: add versioned workout plans
feat: persist workout sessions
feat: add workout media and feed
feat: implement progression ledger
feat: add friends and groups
feat: add rankings and notifications
feat: add advanced progression and hall of fame
```

Um commit só pode ser considerado pronto quando `mvn verify` estiver verde, o
diff staged contiver apenas a fatia anunciada e o contrato usado pelo frontend
estiver documentado.

## Checklist de handoff

- [ ] Revisar a migration e o modelo de domínio da fatia.
- [ ] Confirmar autorização e ownership em todos os endpoints.
- [ ] Confirmar idempotência e comportamento sob concorrência.
- [ ] Executar `mvn verify`.
- [ ] Testar com PostgreSQL local e Flyway desde um banco vazio.
- [ ] Comparar respostas com os contratos em `gymrats-rpg-web/docs`.
- [ ] Atualizar Swagger e README quando necessário.
- [ ] Conferir `git diff --cached` antes do commit.
- [ ] Abrir PR separado e atualizar a tarefa correspondente no ClickUp.
