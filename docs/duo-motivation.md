# Dupla privada e motivação pessoal

Esta entrega adiciona os módulos `duo` e `motivation`. Ambos exigem usuário autenticado e usam o mesmo PostgreSQL e provedor de mídia da aplicação. O modo de apresentação Fitness/RPG não muda metas, permissões, dias contabilizados ou seleção elegível.

## Migration V16 e atualização conjunta

`V16__private_duo_and_motivation.sql` cria:

- `duo_partnerships`, `duo_members` e `duo_invites`: vínculo, participação ativa e convite temporário;
- `duo_meetings`: propostas e respostas de encontros;
- `duo_memories`, `duo_private_media` e `duo_reactions`: lembranças, cópias privadas de fotos e incentivos;
- `motivation_preferences`: um título, uma moldura e um item selecionados por conta.

`duo_members.active_user_id` é único: uma pessoa não participa de duas duplas ativas. As restrições de lembranças impedem duplicação por sessão/dupla e por chave de idempotência/autor/dupla. Índices atendem consultas recentes de lembranças, encontros e mídia por parceria.

Atualize a API, aplique as migrations com Flyway e publique o frontend compatível. A coleção e a dupla não possuem fallback de mock no BFF. Nenhuma variável de ambiente nova é necessária; a cópia privada usa o `MEDIA_STORAGE_PROVIDER` já configurado. O bucket deve continuar privado. Backup do PostgreSQL não inclui os objetos desse armazenamento.

## Endpoints da dupla

Prefixo: `/api/v1/duo`.

| Método e sufixo                             | Contrato                                                                                                                                                                    |
| ------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `GET /`                                     | Painel com `status` (`NONE`, `WAITING`, `ACTIVE`), membros, progresso, encontros, lembranças, preferências, lembretes e resumo mensal.                                      |
| `POST /invites`                             | `201 {token, expiresAt}`. Token hexadecimal de 64 caracteres, válido por sete dias. Reutiliza convite vigente.                                                              |
| `DELETE /invites`                           | `204`. Cancela somente convites pendentes do próprio emissor; não desfaz uma parceria formada em paralelo.                                                                  |
| `GET /invites/{token}`                      | `{available, inviterName, expiresAt}`, sem acesso ao painel privado.                                                                                                        |
| `POST /invites/{token}/join`                | Forma dupla de exatamente duas pessoas e retorna o painel; reenvio pelo participante já conectado é idempotente.                                                            |
| `DELETE /`                                  | `204`. Encerra explicitamente a parceria ativa de ambos.                                                                                                                    |
| `POST /meetings`                            | `{scheduledAt, location?, note?}`; retorna painel.                                                                                                                          |
| `PATCH /meetings/{id}`                      | `{action, scheduledAt?, location?, note?, expectedScheduledAt?}` com `ACCEPT`, `DECLINE` ou `RESCHEDULE`; decisões exigem o horário visto, conforme abaixo. Retorna painel. |
| `GET /workouts`                             | Até 30 sessões concluídas recentes do próprio usuário, com `hasPhoto` e `shared`.                                                                                           |
| `POST /memories`                            | `{sessionId, caption?, includePhoto?}` e header `Idempotency-Key`; retorna painel.                                                                                          |
| `DELETE /memories/{id}`                     | `204`; apenas o autor remove a lembrança.                                                                                                                                   |
| `PUT /memories/{id}/reaction`               | `{type}` com `PROUD`, `TOGETHER` ou `YOU_CAN`; retorna painel.                                                                                                              |
| `DELETE /memories/{id}/reaction?type=PROUD` | Remove a própria reação; retorna painel.                                                                                                                                    |
| `PATCH /settings`                           | `{remindersEnabled?, encouragementEnabled?}`; altera apenas a preferência do usuário e retorna painel.                                                                      |
| `GET /media/{id}`                           | Bytes de foto com `Cache-Control: no-store`; requer vínculo ativo com a dupla proprietária.                                                                                 |

Os endpoints sem status explícito na tabela retornam `200` em sucesso. A API rejeita o próprio convite, a terceira pessoa e membros já conectados a outra dupla. As mutações bloqueiam os registros pertinentes para serializar alterações de vínculo.

Ao ingressar na parceria, os demais convites pendentes das duas pessoas são revogados; sair não reativa esses links. O cliente deve distinguir cancelar convite (`DELETE /invites`) de encerrar parceria (`DELETE /`), inclusive quando a tela está desatualizada.

## Metas e encontros

As metas continuam individuais e versionadas no perfil. O painel calcula dias distintos com sessão `COMPLETED`, usando `America/Sao_Paulo` e semana iniciada na segunda-feira. A razão de cada pessoa é limitada a 1; o progresso cooperativo é a média das razões. A semana só está concluída para a dupla quando ambas alcançam suas próprias metas. Exceder a meta de uma pessoa não compensa a da outra.

Encontros recebem `scheduledAt` futuro até 366 dias adiante, `location` com até 120 caracteres e `note` com até 280. A outra pessoa aceita ou recusa a proposta enviando `expectedScheduledAt` igual ao horário exato que foi apresentado. Ausência desse campo em `ACCEPT`/`DECLINE` retorna `400`; horário desatualizado após um reagendamento retorna `409`. Reagendar devolve a proposta ao estado `PENDING` e exige nova resposta. A listagem do painel é limitada a 30 encontros: propostas pendentes e encontros aceitos futuros aparecem primeiro, do mais próximo ao mais distante; depois vem o histórico em ordem de horário decrescente.

`reminders` consulta independentemente até 30 encontros aceitos e futuros nos próximos sete dias, em ordem crescente de horário, respeitando `remindersEnabled`. Não depende do histórico limitado de encontros: eventos distantes não ocultam um lembrete próximo. É **somente in-app**; não existe scheduler, entrega de push/e-mail/SMS nem criação em calendário externo. Desativar incentivos oculta as reações para o usuário e bloqueia novas adições por ele, sem apagar as reações anteriores ou alterar a preferência da outra pessoa.

## Fotos, autoria e revogação

Uma lembrança exige sessão concluída do autor, legenda de até 280 caracteres e chave de idempotência não vazia de até 120 caracteres. Cada sessão aparece no máximo uma vez por dupla. Uma chave já usada em outra sessão retorna conflito.

Fotos são opcionais e exigem `includePhoto: true`. O serviço copia a foto original do treino para uma chave própria em `duo_private_media`; não reutiliza o acesso público do feed. A cópia segue o provedor de armazenamento existente. O autor controla o envio; concluir treino, check-in ou upload não publica lembranças automaticamente.

Publicar na dupla é independente de publicar no feed. Excluir a lembrança não exclui uma publicação pública anterior; apagar ou substituir a foto original do treino não altera a cópia privada já compartilhada.

Ao sair, ambos perdem imediatamente o vínculo autorizado após o commit. Lembranças, reações, encontros, convites vinculados e metadados de mídia são removidos. As chaves privadas são apagadas do provedor após o commit. Se a exclusão física falhar, o objeto fica sem autorização e pode precisar de reconciliação; não reintroduzir URL pública como fallback. O mesmo encerramento ocorre durante exclusão de conta, que também remove as preferências de motivação. Treinos pessoais, progressão e publicações públicas independentes são preservados.

## Motivação e seleção de recompensas

`GET /api/v1/motivation` retorna `nextStep`, `returningAfterPause`, `daysSinceLastWorkout`, `rewards` e `selected`.

O catálogo inclui oito recompensas com `progressUnit: "dias"` e dois títulos legados com `progressUnit: "conquista"`. Os marcos de dias usam o total de dias distintos com atividade concluída do usuário, em `America/Sao_Paulo`; vários check-ins no mesmo dia contam uma vez. Não há XP adicional, penalidade por pausa ou regra diferente por tema.

| Tipo    | ID                | Dias distintos |
| ------- | ----------------- | -------------- |
| `TITLE` | `Desperto`        | 1              |
| `TITLE` | `Disciplinado`    | 7              |
| `TITLE` | `Implacável`      | 30             |
| `FRAME` | `vanguard`        | 1              |
| `FRAME` | `ascendant`       | 30             |
| `ITEM`  | `constancy-pin`   | 3              |
| `ITEM`  | `journey-compass` | 14             |
| `ITEM`  | `sunrise-charm`   | 60             |

O título `Desperto` também respeita um desbloqueio `FIRST_WORKOUT` já auditado, garantindo ao menos 1 em `current` nesse caso. Além da tabela acima, o catálogo preserva:

| Tipo    | ID           | Registro em `user_achievements` | Requisito original                              |
| ------- | ------------ | ------------------------------- | ----------------------------------------------- |
| `TITLE` | `Vanguarda`  | `WEEKLY_THREE`                  | Treinar em três dias distintos na mesma semana. |
| `TITLE` | `Ascendente` | `XP_500`                        | Acumular 500 XP validados.                      |

Os dois títulos legados usam `progressUnit: "conquista"`, `target: 1` e `current: 0` ou `1`, de acordo com a conquista auditada. Não são reinterpretados como marcos de 7/30 dias. Os IDs de título `Vanguarda` e `Ascendente` não são as molduras `vanguard` e `ascendant`.

Cada recompensa informa `id`, `type`, `name`, `description`, `requirement`, `current`, `target`, `progressUnit`, `unlocked`, `selected` e `visualKey`. O frontend pode localizar os nomes, mas deve preservar os IDs, a unidade e as condições da API; para os títulos legados, deve mostrar o requisito original em vez de uma contagem de dias.

O próximo passo escolhe o primeiro alvo ainda não atingido em `1, 3, 7, 14, 30, 60, 90, 180`, seguido do próximo múltiplo de 30. `returningAfterPause` fica verdadeiro após sete dias sem conclusão; `daysSinceLastWorkout` é `null` para quem ainda não concluiu atividade. Esse sinal orienta uma mensagem acolhedora e não reinicia a contagem.

`PATCH /api/v1/motivation/rewards/selection` recebe `titleId?`, `frameId?` e `itemId?`. Campos omitidos ou `null` mantêm a escolha. Remoção usa `titleId: ""`, `frameId: "none"` e `itemId: "none"`. Os limites são 80 caracteres para título e 40 para moldura/item.

A API valida ID, tipo e desbloqueio, grava em `motivation_preferences` sob bloqueio do usuário e retorna o estado completo. Também sincroniza `User.selectedTitle`; o endpoint legado de título usa essa mesma seleção autoritativa e a elegibilidade correspondente, por dias ou conquista auditada. Valores desconhecidos ou bloqueados retornam `409`. O banco é a autoridade; o frontend não deve substituir uma escolha recusada por um estado apenas local.

Sem linha em `motivation_preferences`, `GET /api/v1/motivation` preserva o título legítimo já selecionado em `users.selected_title`, desde que elegível, inclusive para contas anteriores à V16. Não cria nem zera preferências só pela leitura; moldura/item assumem `none`, e título inválido assume vazio.

Parâmetros UUID/enum inválidos e query strings obrigatórias ausentes retornam `400` pelo tratamento compartilhado de erros. Conflitos de estado, horário de proposta desatualizado e personalizações não elegíveis retornam `409`.

## Verificação

`mvn verify` inclui `DuoMotivationIntegrationTest`, com cenários de convite, metas independentes, idempotência, autoria, preferências, mídia privada, revogação após saída e elegibilidade de recompensas. O perfil de testes usa H2; a migration também precisa ser ensaiada em PostgreSQL no ambiente integrado. As verificações de interface ficam no repositório web, incluindo o fluxo `/duo/invite/{token}`, o mural privado e `/rewards`.
