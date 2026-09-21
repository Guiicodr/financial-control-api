# API de Controle Financeiro

API REST para gestão financeira pessoal, desenvolvida em Java com Spring Boot. A aplicação cobre autenticação, transações, objetivos, orçamento, projeções, notificações e integração com WhatsApp.

## Linguagem principal

- Java 21

## Stack e tecnologias utilizadas

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- PostgreSQL como banco principal em produção
- H2 Database para testes e ambiente local/dev
- JWT com jjwt
- Maven para build e gestão de dependências
- JSON para payloads da API e tokens JWT
- CORS e rate limiting customizados
- Integração com WhatsApp via webhook (Twilio/Meta)

## Estrutura principal do projeto

```text
src/
├── main/
│   ├── java/
│   │   └── com/guilherme/controlefinanceiro/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── model/
│   │       ├── repository/
│   │       └── service/
│   └── resources/
│       ├── application.properties
│       └── application-dev.properties
└── test/
    └── java/
```

## Banco de dados

A configuração padrão da aplicação usa PostgreSQL:

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/controlefinanceiro}
spring.datasource.driver-class-name=org.postgresql.Driver
```

O H2 é usado apenas no ambiente de teste e em configurações específicas do projeto. O profile de teste define `jdbc:h2:mem:controlefinanceiro`.

## Segurança e autenticação

- Spring Security
- JWT para autenticação de usuários
- Password encoding com BCrypt
- CORS configurado explicitamente
- Rate limiting para endpoints públicos e webhook

## Funcionalidades principais

- Registro, login e refresh de autenticação
- Gestão de transações financeiras
- Cálculo de saldo
- Gestão de objetivos financeiros
- Cartões de crédito
- Orçamentos e alertas
- Projeções financeiras
- Notificações
- Vinculação de WhatsApp ao usuário
- Webhook de WhatsApp para entrada de mensagens
- Direitos do titular (LGPD art. 18): exportar os próprios dados, excluir a conta e desvincular o WhatsApp

## Endpoints principais

### Autenticação

```text
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/forgot-password
POST /auth/reset-password
```

O `POST /auth/register` exige o campo `aceiteVersao` (a versão dos Termos de Uso e
do Aviso de Privacidade exibidos no app). Sem ele a API responde `400` e nada é
gravado — é o que torna o consentimento comprovável (LGPD art. 8º, §1º). A versão
aceita e o instante do aceite ficam no usuário e aparecem em `GET /usuario/dados`.

### Transações

```text
POST   /transacoes
GET    /transacoes
GET    /transacoes/saldo
GET    /transacoes/monthly
PUT    /transacoes/{id}
DELETE /transacoes/{id}
```

### Objetivos

```text
POST   /objetivos
GET    /objetivos
DELETE /objetivos/{id}
```

### Outros módulos

```text
GET    /cartoes
POST   /cartoes
DELETE /cartoes/{id}

GET    /income
POST   /income
PUT    /income/base
POST   /income/extras
DELETE /income/{id}

GET    /orcamentos
POST   /orcamentos
GET    /orcamentos/alertas

GET    /projecoes/saldo

GET    /notificacoes

GET    /usuario/whatsapp
POST   /usuario/whatsapp
DELETE /usuario/whatsapp

# Direitos do titular (LGPD art. 18)
GET    /usuario/dados
DELETE /usuario

GET    /webhooks/whatsapp
POST   /webhooks/whatsapp
```

## Usuário de teste

O usuário de teste é criado apenas no profile `dev`:

- E-mail: `teste@teste.com`
- Senha: `teste123`

Esse seed é controlado pela propriedade:

```properties
app.dev.seed-test-user=true
```

## Como rodar

1. Clonar o repositório
2. Configurar as variáveis de ambiente do banco e JWT
3. Executar:

```bash
./mvnw spring-boot:run
```

Ou usando Maven:

```bash
./mvnw clean install
java -jar target/controlefinanceiro-0.0.1-SNAPSHOT.jar
```

A API fica disponível em:

```text
http://localhost:8080
```

## Variáveis de ambiente importantes

```properties
JWT_SECRET
JWT_EXPIRATION_MS
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
WHATSAPP_WEBHOOK_TOKEN
WHATSAPP_TOKEN
WHATSAPP_PHONE_NUMBER_ID
WHATSAPP_BOT_NUMBER
CORS_ALLOWED_ORIGINS
LEGAL_TERMS_VERSION
RETENTION_TOKEN_CLEANUP_CRON
```

## Observações

- A API foi pensada para ser consumida por um frontend separado, não sendo um projeto fullstack monolítico.
- A configuração de produção usa PostgreSQL, enquanto o H2 fica no ambiente de teste/local.
- A integração com WhatsApp depende de tokens e configurações externas do provedor.

## Privacidade e LGPD

A API trata dados financeiros e de autenticação, que a Resolução CD/ANPD nº
15/2024 lista como gatilho de comunicação obrigatória em caso de incidente. Os
direitos do titular do art. 18 são atendidos dentro do próprio produto:

- **Acesso e portabilidade**: `GET /usuario/dados` devolve, em JSON, o cadastro e
  todo o histórico de quem chamou (sem hash de senha). O arquivo é montado no
  navegador do titular, então não fica retido no servidor.
- **Eliminação**: `DELETE /usuario` apaga conta e histórico em cascata, exigindo a
  senha no corpo — a operação é irreversível e o token de sessão pode estar em um
  navegador alheio.
- **Revogação do consentimento**: `DELETE /usuario/whatsapp` remove o número do
  banco sem excluir a conta.
- **Prova do consentimento**: versão e instante do aceite gravados no cadastro
  (art. 8º, §1º).

Os três endpoints operam sempre sobre o usuário autenticado, nunca sobre um id
recebido no caminho ou no corpo: não existe caminho para um titular alcançar
dados de outro.

### Documentos internos

| Documento | Conteúdo |
|---|---|
| `docs/privacidade/ROPA.md` | Registro das operações de tratamento (art. 37) |
| `docs/privacidade/POLITICA-DE-RETENCAO.md` | Prazos de guarda e descarte (arts. 15 e 16) |
| `docs/privacidade/PLANO-DE-RESPOSTA-A-INCIDENTES.md` | Prazos e conteúdo da comunicação (art. 48; Res. CD/ANPD nº 15/2024) |
| `docs/privacidade/CHECKLIST-LGPD.md` | O que está conforme, o que falta e as evidências |

Os textos publicados no app (Termos de Uso e Aviso de Privacidade) são versionados
em `LEGAL_TERMS_VERSION`; ao publicar uma versão nova, mantenha o valor igual ao
`LEGAL_VERSION` do front.

### Retenção e higiene dos logs

`TokenCleanupJob` expurga, todo dia, refresh tokens e tokens de recuperação de
senha já vencidos (cron em `RETENTION_TOKEN_CLEANUP_CRON`, padrão 03:30). Os logs
da aplicação não carregam dado pessoal em claro: `util/PiiMasker` centraliza o
mascaramento de e-mail e telefone.

### Testes

```bash
./mvnw test
```

`LgpdApiTests` cobre cadastro sem aceite (400), registro do aceite, exportação
restrita ao titular, exclusão com senha, revogação do WhatsApp e expurgo
seletivo; `PiiMaskerTests` fixa o formato do mascaramento.

## Status

Em desenvolvimento ativo.