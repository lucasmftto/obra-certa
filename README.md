# Obra Certa 🔧

Sistema de gerenciamento de orçamentos e controle de gastos em reformas residenciais.

## Sobre o projeto

O **Obra Certa** permite cadastrar projetos de reforma (casas e apartamentos), organizar ambientes, controlar itens orçados e registrar gastos efetivos — dando uma visão clara do progresso financeiro e físico da obra.

### Funcionalidades

- Cadastro de projetos com status de ciclo de vida (Em orçamento → Em andamento → Pausado → Concluído)
- Organização por ambientes (sala, cozinha, quarto etc.)
- Itens com orçamento calculado (quantidade × preço unitário)
- Registro de gastos reais por item com data e categoria
- Flags de **concluído** e **atrasado** por item, com recálculo automático do progresso
- Alertas visuais de orçamento estourado e itens com atraso
- Filtros por status, tipo (casa/apartamento) e busca por nome/endereço

---

## Stack

### Backend
| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.2.5 |
| Spring Data JPA / Hibernate | — |
| PostgreSQL | 16 |
| Flyway | — |
| MapStruct | 1.5.5 |
| Bean Validation | — |

### Frontend
| Tecnologia | Versão |
|---|---|
| Angular | 17 |
| Angular Material | 17 |
| TypeScript | 5.x |

---

## Pré-requisitos

- Java 21+
- Node.js 18+
- Docker e Docker Compose

---

## Como rodar

### 1. Banco de dados (PostgreSQL via Docker)

```bash
docker compose up -d
```

Isso sobe um PostgreSQL na porta `5432` com:
- Database: `obra_certa_db`
- Usuário: `obra_certa_user`
- Senha: `obra_certa_pass`

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8080`.  
As migrações Flyway são aplicadas automaticamente na inicialização.

### 3. Frontend

```bash
cd frontend
npm install
ng serve
```

A aplicação abre em `http://localhost:4200`.

---

## Estrutura do projeto

```
obra-certa/
├── backend/                  # Spring Boot API
│   └── src/
│       ├── main/java/com/obracerta/
│       │   ├── project/      # Projetos
│       │   ├── environment/  # Ambientes
│       │   ├── item/         # Itens
│       │   ├── expense/      # Gastos
│       │   ├── category/     # Categorias
│       │   └── shared/       # Config, exceções, DTOs comuns
│       └── resources/
│           └── db/migration/ # Scripts Flyway (V1–V7)
└── frontend/                 # Angular SPA
    └── src/app/
        ├── core/             # Services, models, pipes, interceptors
        └── features/
            └── projetos/     # Pages e components
```

---

## API — Principais endpoints

| Método | URL | Descrição |
|---|---|---|
| `GET` | `/api/v1/projects` | Lista projetos (filtros: status, type, search) |
| `POST` | `/api/v1/projects` | Cria projeto |
| `GET` | `/api/v1/projects/{id}` | Detalhe do projeto |
| `PUT` | `/api/v1/projects/{id}` | Atualiza projeto |
| `PATCH` | `/api/v1/projects/{id}/status` | Muda status |
| `DELETE` | `/api/v1/projects/{id}` | Remove projeto (soft delete) |
| `GET` | `/api/v1/projects/{id}/summary` | Resumo financeiro |
| `GET/POST` | `/api/v1/projects/{id}/environments` | Ambientes do projeto |
| `GET/PUT/DELETE` | `/api/v1/environments/{id}` | CRUD ambiente |
| `GET/POST` | `/api/v1/environments/{id}/items` | Itens do ambiente |
| `GET/PUT/DELETE` | `/api/v1/items/{id}` | CRUD item |
| `PATCH` | `/api/v1/items/{id}/flags` | Atualiza concluído/atrasado |
| `GET/POST` | `/api/v1/items/{id}/expenses` | Gastos do item |
| `GET/PUT/DELETE` | `/api/v1/expenses/{id}` | CRUD gasto |
| `GET` | `/api/v1/categories` | Lista categorias |

---

## Modelo de dados

```
Projeto
  └── Ambiente (1..N)
        └── Item (1..N)
              └── Gasto/Expense (0..N)
```

**Status do projeto:** `IN_BUDGET` → `IN_PROGRESS` ⇄ `ON_HOLD` → `COMPLETED` → `IN_PROGRESS`

---

## Testes

```bash
cd backend
./mvnw test
```

38 testes unitários cobrindo services e controllers. Testes de integração usam **TestContainers** com PostgreSQL real.

> **Nota:** Requer Docker rodando. Configurar `~/.testcontainers.properties` com `tc.host=unix:///var/run/docker.sock` no macOS com Docker Desktop 29+.
