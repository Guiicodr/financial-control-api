# Financial Control API

REST API for a personal financial assistant built with Java and Spring Boot.

The API manages financial transactions, categories, balance calculation, and financial goals. It is designed to be consumed by a frontend dashboard application.

## Features

- Create financial transactions
- List financial transactions
- Delete financial transactions
- Calculate current balance
- Support transaction categories
- Create financial goals
- List financial goals
- Delete financial goals
- Track goal progress through frontend integration

## Technologies

- Java
- Spring Boot
- Spring Data JPA
- H2 Database
- Maven

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/guilherme/controlefinanceiro/
│   │       ├── controller/
│   │       ├── model/
│   │       ├── repository/
│   │       └── service/
│   └── resources/
│       └── application.properties
```

## Main Entities

### Transaction

Represents a financial transaction.

Fields:

- id
- description
- value
- type
- date
- category

Transaction types:

```text
ENTRADA
SAIDA
```

Categories:

```text
ALIMENTACAO
TRANSPORTE
LAZER
ESTUDOS
SALARIO
OUTROS
```

### Goal

Represents a financial goal.

Fields:

- id
- name
- target value
- current value
- deadline
- type

Goal types:

```text
COMPRA
ECONOMIA
INVESTIMENTO
RESERVA
```

## API Endpoints

### Transactions

```text
GET    /transacoes
POST   /transacoes
DELETE /transacoes/{id}
GET    /transacoes/saldo
```

Example request:

```json
{
  "descricao": "Market",
  "valor": 120,
  "tipo": "SAIDA",
  "data": "2026-06-02",
  "categoria": "ALIMENTACAO"
}
```

### Goals

```text
GET    /objetivos
POST   /objetivos
DELETE /objetivos/{id}
```

Example request:

```json
{
  "nome": "Buy a notebook",
  "valorAlvo": 4000,
  "valorAtual": 1200,
  "prazo": "2026-12-31",
  "tipo": "COMPRA"
}
```

## How to Run

1. Clone the repository:

```bash
git clone REPOSITORY_URL
```

2. Open the project in your IDE.

3. Run the Spring Boot application.

4. The API will be available at:

```text
http://localhost:8080
```

## Database

The project currently uses H2 Database for development.

Data is stored in memory, so it may be reset when the application restarts.

## Frontend

This API is consumed by a React frontend application.

Frontend repository:

```text
financial-control-dashboard
```

## Status

In development.