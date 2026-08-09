# Deploy do beta gratuito

Arquitetura: Vercel (Next.js) + Google Cloud Run (Spring Boot) + Supabase
(PostgreSQL e Storage). Nenhum segredo deve ser commitado.

## 1. Supabase

1. Crie um projeto gratuito e guarde a senha do banco.
2. Em **Storage**, crie um bucket **privado** chamado `gymrats-media`.
3. Em **Project Settings > Database**, copie os dados do **Session pooler**.
   O Cloud Run pode não alcançar a conexão IPv6 direta do banco.
4. Em **Project Settings > API Keys**, copie a URL do projeto e a chave
   `service_role`. Essa chave pertence exclusivamente à API.

Valores que serão usados no Cloud Run:

```text
DB_URL=jdbc:postgresql://HOST_DO_SESSION_POOLER:5432/postgres?sslmode=require
DB_USERNAME=USUARIO_EXIBIDO_PELO_SUPABASE
DB_PASSWORD=SENHA_DO_BANCO
MEDIA_STORAGE_PROVIDER=supabase
SUPABASE_URL=https://PROJECT_REF.supabase.co
SUPABASE_STORAGE_BUCKET=gymrats-media
SUPABASE_SERVICE_ROLE_KEY=CHAVE_SERVICE_ROLE
```

O Flyway cria e atualiza as tabelas automaticamente na primeira inicialização.

## 2. Cloud Run

No Google Cloud:

1. Crie ou selecione um projeto com faturamento habilitado.
2. Ative Cloud Run, Cloud Build e Artifact Registry.
3. Abra o Cloud Shell, clone o repositório da API e entre na pasta que contém
   o `Dockerfile`.
4. Execute:

```bash
gcloud run deploy gymrats-rpg-api \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --cpu 1 \
  --memory 1Gi \
  --min-instances 0 \
  --max-instances 1 \
  --cpu-boost \
  --concurrency 40 \
  --timeout 60
```

`--cpu-boost` reduz o tempo de inicialização sem manter uma instância ociosa.
Com `--min-instances 0`, a primeira chamada após um período sem uso ainda pode
sofrer cold start. Para eliminar esse atraso, altere para `--min-instances 1`,
ciente de que instâncias mínimas geram cobrança mesmo quando estão ociosas.

O acesso público do Cloud Run é necessário para o frontend alcançar a API; os
endpoints privados continuam protegidos pelo JWT da aplicação.

Em **Edit and deploy new revision > Variables and secrets**, cadastre os valores
do Supabase acima e também:

```text
JWT_SECRET=BASE64_ALEATORIO_DE_32_BYTES
JWT_EXPIRATION=604800
FRONTEND_ORIGIN=https://SEU_PROJETO.vercel.app
```

Prefira o Secret Manager para `DB_PASSWORD`, `JWT_SECRET` e
`SUPABASE_SERVICE_ROLE_KEY`. Gere o JWT no PowerShell sem publicar o resultado:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
[Convert]::ToBase64String($bytes)
```

Teste a URL retornada pelo deploy:

```text
https://URL_DO_CLOUD_RUN/actuator/health
```

A resposta esperada é `{"status":"UP"}`.

## 3. Vercel

1. Importe o repositório `gymrats-rpg-web`.
2. O preset deve ser **Next.js** e o plano **Hobby**.
3. Cadastre estas variáveis para Production, Preview e Development:

```text
API_URL=https://URL_DO_CLOUD_RUN/api/v1
MOCK_API=false
NEXT_PUBLIC_APP_NAME=Awaken
NEXT_PUBLIC_APP_SHORT_NAME=Awaken
```

4. Faça o deploy e copie a URL `https://...vercel.app`.
5. Volte ao Cloud Run, ajuste `FRONTEND_ORIGIN` para essa URL e publique uma
   nova revisão.

Não cadastre a chave `service_role`, senha do banco ou `JWT_SECRET` na Vercel.

## 4. Checklist de validação

- `/actuator/health` responde `UP`.
- O frontend abre sem `MOCK_API`.
- Cadastro, login e onboarding funcionam.
- Um treino pode ser concluído e uma foto enviada.
- A foto continua disponível depois de publicar uma nova revisão da API.
- Exclusão da foto e da conta removem o objeto do bucket.
- Os limites de gastos do Google Cloud estão configurados com alertas.

## Desenvolvimento local

O padrão continua sendo `MEDIA_STORAGE_PROVIDER=local`, usando
`MEDIA_STORAGE_PATH`. Portanto, Supabase não é necessário para desenvolver ou
executar os testes localmente.
