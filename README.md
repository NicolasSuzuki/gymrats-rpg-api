# GymRats RPG — API

API REST em Spring Boot para o aplicativo GymRats RPG. O projeto é um monólito modular: cada domínio agrupa API, aplicação e persistência; regras de negócio permanecem no backend.

## Requisitos

- Java 17+
- Maven 3.9+
- Docker, opcional, para o PostgreSQL local

## Execução local

```bash
cp .env.example .env
docker compose up -d
set -a && source .env && set +a
mvn spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

## Fluxo autenticado

1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login`
3. Enviar `Authorization: Bearer <token>` em `GET /api/v1/auth/me`

O JWT expira em sete dias por padrão e pode ser configurado por `JWT_EXPIRATION`. No frontend Next.js, o token é mantido somente em cookie `HttpOnly`, `SameSite=Lax` e `Secure` em produção.

Refresh token, login social, confirmação de e-mail e 2FA não pertencem ao MVP atual.

## Validação

```bash
mvn verify
```

## Deploy do beta

O roteiro para Vercel, Cloud Run, PostgreSQL e Supabase Storage está em
[`docs/deploy-beta-gratuito.md`](docs/deploy-beta-gratuito.md).

## Mídia e feed

- `POST /api/v1/workout-sessions/{id}/photo`: envia JPG, PNG ou WebP de até 5 MB.
- `DELETE /api/v1/workout-sessions/{id}/photo`: remove a foto do próprio treino.
- `POST /api/v1/workout-sessions/{id}/publish`: publica um treino concluído com `Idempotency-Key`.
- `GET /api/v1/feed`: lista publicações com cursor, `pageSize` e filtro `category`.
- `GET|POST /api/v1/feed/{id}/comments`: consulta ou cria comentários.
- `DELETE /api/v1/feed/{postId}/comments/{commentId}`: remove apenas comentário do autor autenticado.
- `GET /api/v1/profile/workout-diary`: consulta o diário do usuário autenticado.

O desenvolvimento local usa `MEDIA_STORAGE_PROVIDER=local` e `MEDIA_STORAGE_PATH`.
No beta, use `MEDIA_STORAGE_PROVIDER=supabase`; a API mantém a chave privilegiada
somente no servidor. Metadados, checksum SHA-256, autoria e vínculo com a sessão
continuam no PostgreSQL.

## Progressão avançada

- `GET /api/v1/progression/advanced`: retorna a maestria independente das seis classes, o sub-rank atual e o histórico auditável.
- `PATCH /api/v1/progression/advanced/pause`: congela ou retoma uma avaliação legítima, preservando o tempo já cumprido.
- `GET /api/v1/progression/hall-of-fame`: elege atomicamente o Monarca atual e retorna todos os reinados preservados.

Cada treino concluído concede XP à classe ativa no momento do check-in. Trocar de classe não remove nem transfere o histórico já conquistado. Após o rank A, a consistência semanal conduz pelos sub-ranks S-5 até S-1. A permanência por 30 dias ativos em S-1 promove para Nacional; pausas informadas pelo usuário congelam esse relógio.

Os limites semanais, o período protegido e o critério de desempate são configuráveis pelas variáveis `ADVANCED_RANK_*` documentadas em `.env.example`. O padrão de desempate é: mais dias distintos com check-in e, depois, maior quantidade de check-ins.

Entre usuários no rank Nacional, apenas o primeiro elegível pode manter o título de Monarca. A transferência usa bloqueio pessimista sobre um estado singleton e uma restrição única no banco, impedindo dois reinados ativos. Cada período registra início e fim; o Hall da Fama agrega quantidade de reinados, maior duração, duração total e todas as datas, inclusive após a perda do título.

## Variáveis

Veja `.env.example`. Em ambientes reais, substitua obrigatoriamente `JWT_SECRET` por um segredo Base64 seguro de pelo menos 256 bits e nunca o versione.

Exemplo para gerar um segredo local:

```bash
openssl rand -base64 32
```

### Erro `Illegal base64 character` ao iniciar a API

Se a inicialização falhar em `JwtService` com uma mensagem semelhante a
`io.jsonwebtoken.io.DecodingException: Illegal base64 character`, o valor de
`JWT_SECRET` não está em Base64 padrão. Isso costuma acontecer quando o valor
do `.env` ainda é um placeholder ou foi gerado em Base64URL.

No PowerShell, gere e carregue um segredo Base64 seguro de 256 bits com:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$jwtSecret = [Convert]::ToBase64String($bytes)
$env:JWT_SECRET = $jwtSecret
```

Copie o valor de `$jwtSecret` para `JWT_SECRET` no arquivo `.env`, sem aspas ou
espaços adicionais, e execute novamente `mvn spring-boot:run`. O `.env` contém
segredos locais e não deve ser adicionado ao Git.

## Organização

- `auth`: cadastro e login.
- `user`: usuário e papéis.
- `exercise`: catálogo global e pessoal.
- `plan`: planos de treino versionados.
- `workout`: sessões, autosave e conclusão idempotente.
- `feed`: fotos, publicações, diário e comentários.
- `security`: JWT e configuração de acesso.
- `shared`: auditoria e respostas de erro.
- `config`: configurações transversais.

Branches curtas, Conventional Commits, Pull Request e revisão do Nicolas são obrigatórios.
