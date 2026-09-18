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

## Endpoints principais

### Autenticação

```text
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/forgot-password
POST /auth/reset-password
```

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
```

## Observações

- A API foi pensada para ser consumida por um frontend separado, não sendo um projeto fullstack monolítico.
- A configuração de produção usa PostgreSQL, enquanto o H2 fica no ambiente de teste/local.
- A integração com WhatsApp depende de tokens e configurações externas do provedor.

## Status

Em desenvolvimento ativo.