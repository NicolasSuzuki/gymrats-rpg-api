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

## Variáveis

Veja `.env.example`. Em ambientes reais, substitua obrigatoriamente `JWT_SECRET` por um segredo Base64 seguro de pelo menos 256 bits e nunca o versione.

Exemplo para gerar um segredo local:

```bash
openssl rand -base64 32
```

## Organização

- `auth`: cadastro e login.
- `user`: usuário e papéis.
- `security`: JWT e configuração de acesso.
- `shared`: auditoria e respostas de erro.
- `config`: configurações transversais.

Branches curtas, Conventional Commits, Pull Request e revisão do Nicolas são obrigatórios.
