# Obra Certa — Spec de API: Módulo Projetos

## Base URL

```
/api/v1/projetos
```

---

## Endpoints

### 1. Listar projetos

```
GET /api/v1/projetos
```

**Query params:**

| Param | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `status` | string | Não | Filtra por status: `EM_ORCAMENTO`, `EM_ANDAMENTO`, `PAUSADO`, `CONCLUIDO` |
| `tipo` | string | Não | Filtra por tipo: `CASA`, `APARTAMENTO` |
| `busca` | string | Não | Busca por nome ou endereço (case-insensitive, contains) |
| `page` | int | Não | Página (default: 0) |
| `size` | int | Não | Itens por página (default: 20, max: 50) |
| `sort` | string | Não | Campo de ordenação (default: `criadoEm,desc`) |

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "nome": "Reforma apartamento Moema",
      "tipo": "APARTAMENTO",
      "endereco": "Rua das Flores, 123 — Moema, SP",
      "status": "EM_ANDAMENTO",
      "totalOrcado": 85000.00,
      "totalPrevisto": 40000.00,
      "totalEfetivo": 32400.00,
      "saldoRestante": 52600.00,
      "percentualEfetivo": 38.12,
      "percentualConcluido": 35.00,
      "totalAmbientes": 5,
      "criadoEm": "2024-03-01T10:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

### 2. Buscar projeto por ID

```
GET /api/v1/projetos/{id}
```

**Response 200:**
```json
{
  "id": 1,
  "nome": "Reforma apartamento Moema",
  "tipo": "APARTAMENTO",
  "endereco": "Rua das Flores, 123 — Moema, SP",
  "descricao": "Reforma completa, incluindo cozinha e banheiros.",
  "status": "EM_ANDAMENTO",
  "totalOrcado": 85000.00,
  "totalPrevisto": 40000.00,
  "totalEfetivo": 32400.00,
  "saldoRestante": 52600.00,
  "percentualEfetivo": 38.12,
  "percentualConcluido": 35.00,
  "emAlerta": false,
  "totalAmbientes": 5,
  "criadoEm": "2024-03-01T10:00:00",
  "atualizadoEm": "2024-03-15T14:30:00"
}
```

**Response 404:**
```json
{
  "status": 404,
  "erro": "Projeto não encontrado",
  "mensagem": "Projeto com id 99 não foi encontrado.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 3. Criar projeto

```
POST /api/v1/projetos
```

**Request body:**
```json
{
  "nome": "Reforma apartamento Moema",
  "tipo": "APARTAMENTO",
  "endereco": "Rua das Flores, 123 — Moema, SP",
  "descricao": "Reforma completa, incluindo cozinha e banheiros."
}
```

**Response 201:**
```json
{
  "id": 1,
  "nome": "Reforma apartamento Moema",
  "tipo": "APARTAMENTO",
  "endereco": "Rua das Flores, 123 — Moema, SP",
  "descricao": "Reforma completa, incluindo cozinha e banheiros.",
  "status": "EM_ORCAMENTO",
  "totalOrcado": 0.00,
  "totalPrevisto": 0.00,
  "totalEfetivo": 0.00,
  "saldoRestante": 0.00,
  "percentualEfetivo": 0.00,
  "percentualConcluido": 0.00,
  "emAlerta": false,
  "totalAmbientes": 0,
  "criadoEm": "2024-03-20T10:00:00",
  "atualizadoEm": "2024-03-20T10:00:00"
}
```

**Response 422 (validação):**
```json
{
  "status": 422,
  "erro": "Dados inválidos",
  "campos": [
    { "campo": "nome", "mensagem": "Nome é obrigatório" },
    { "campo": "tipo", "mensagem": "Tipo deve ser CASA ou APARTAMENTO" }
  ],
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 4. Atualizar projeto

```
PUT /api/v1/projetos/{id}
```

**Request body:** (mesmo schema do POST)

**Response 200:** (mesmo schema do GET por ID)

**Response 409 (projeto concluído):**
```json
{
  "status": 409,
  "erro": "Operação não permitida",
  "mensagem": "Projetos com status CONCLUIDO não podem ser editados.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 5. Atualizar status

```
PATCH /api/v1/projetos/{id}/status
```

**Request body:**
```json
{
  "status": "EM_ANDAMENTO"
}
```

**Response 200:** (mesmo schema do GET por ID com novo status)

**Response 409 (transição inválida):**
```json
{
  "status": 409,
  "erro": "Transição inválida",
  "mensagem": "Não é possível mudar de EM_ORCAMENTO para CONCLUIDO diretamente.",
  "timestamp": "2024-03-20T10:00:00"
}
```

**Response 422 (pré-condição não atendida):**
```json
{
  "status": 422,
  "erro": "Pré-condição não atendida",
  "mensagem": "O projeto precisa ter ao menos um ambiente com um item para iniciar a obra.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 6. Excluir projeto

```
DELETE /api/v1/projetos/{id}
```

**Response 204:** (sem body)

**Response 409 (status não permite exclusão):**
```json
{
  "status": 409,
  "erro": "Operação não permitida",
  "mensagem": "Somente projetos com status EM_ORCAMENTO podem ser excluídos.",
  "timestamp": "2024-03-20T10:00:00"
}
```

---

### 7. Resumo do projeto (alimenta o dashboard)

```
GET /api/v1/projetos/{id}/resumo
```

**Response 200:**
```json
{
  "projetoId": 1,
  "nome": "Reforma apartamento Moema",
  "status": "EM_ANDAMENTO",
  "totais": {
    "orcado": 85000.00,
    "previsto": 40000.00,
    "efetivo": 32400.00,
    "saldo": 52600.00,
    "percentualEfetivo": 38.12,
    "percentualConcluido": 35.00,
    "emAlerta": false
  },
  "porAmbiente": [
    {
      "ambienteId": 1,
      "nome": "Cozinha",
      "orcado": 22000.00,
      "previsto": 20000.00,
      "efetivo": 18500.00,
      "saldo": 3500.00,
      "percentualEfetivo": 84.09,
      "percentualConcluido": 80.00,
      "emAlerta": false
    },
    {
      "ambienteId": 2,
      "nome": "Banheiro social",
      "orcado": 13000.00,
      "previsto": 14000.00,
      "efetivo": 13900.00,
      "saldo": -900.00,
      "percentualEfetivo": 106.92,
      "percentualConcluido": 90.00,
      "emAlerta": true
    }
  ],
  "ultimosGastos": [
    {
      "gastoId": 45,
      "descricao": "Piso porcelanato 60x60",
      "valorPrevisto": 3000.00,
      "valorEfetivo": 3200.00,
      "dataPrevista": "2024-03-15",
      "dataEfetiva": "2024-03-18",
      "pendente": false,
      "ambienteNome": "Cozinha"
    }
  ]
}
```

---

## Tabela resumo de endpoints

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/projetos` | Listar projetos (paginado) |
| `GET` | `/api/v1/projetos/{id}` | Buscar projeto por ID |
| `POST` | `/api/v1/projetos` | Criar projeto |
| `PUT` | `/api/v1/projetos/{id}` | Atualizar projeto |
| `PATCH` | `/api/v1/projetos/{id}/status` | Atualizar status |
| `DELETE` | `/api/v1/projetos/{id}` | Excluir projeto (soft delete) |
| `GET` | `/api/v1/projetos/{id}/resumo` | Resumo financeiro e de progresso |

---

## Códigos de status utilizados

| Código | Uso |
|---|---|
| `200` | Sucesso em GET, PUT, PATCH |
| `201` | Criação bem-sucedida (POST) |
| `204` | Exclusão bem-sucedida (DELETE) |
| `404` | Recurso não encontrado |
| `409` | Conflito de regra de negócio ou transição inválida |
| `422` | Validação de campos ou pré-condição não atendida |
| `500` | Erro interno inesperado |
