# Obra Certa — Spec Funcional: Módulo Projetos

## Visão geral

O módulo de Projetos é o ponto de entrada do sistema. Todo o restante (ambientes, itens, gastos) existe dentro de um projeto. O usuário cria um projeto para cada obra que deseja orçar e acompanhar.

---

## Entidade: Projeto

Representa uma obra de reforma. Pode ser uma casa, apartamento, ou outro imóvel.

---

## Status do projeto

O projeto passa pelos seguintes estados:

```
EM_ORCAMENTO → EM_ANDAMENTO → CONCLUIDO
                     ↓
                 PAUSADO → EM_ANDAMENTO
```

| Status | Descrição |
|---|---|
| `EM_ORCAMENTO` | Projeto criado, orçamento ainda sendo montado. Nenhum gasto lançado. |
| `EM_ANDAMENTO` | Obra iniciada. Gastos podem ser lançados. |
| `PAUSADO` | Obra temporariamente interrompida. Gastos não podem ser lançados. |
| `CONCLUIDO` | Obra finalizada. Nenhuma alteração permitida. |

---

## Regras de negócio

### RN-PROJ-01 — Criação
- Todo projeto deve ter pelo menos nome e tipo (CASA ou APARTAMENTO).
- O status inicial é sempre `EM_ORCAMENTO`.
- A data de criação é preenchida automaticamente pelo sistema.

### RN-PROJ-02 — Edição
- Nome, endereço, tipo e descrição podem ser editados em qualquer status, exceto `CONCLUIDO`.
- O status `CONCLUIDO` bloqueia qualquer edição no projeto e em seus filhos (ambientes, itens, gastos).

### RN-PROJ-03 — Transição de status
- Somente as transições definidas no diagrama acima são permitidas.
- Para avançar de `EM_ORCAMENTO` para `EM_ANDAMENTO`, o projeto deve ter ao menos um ambiente com ao menos um item orçado.
- Não é permitido voltar de `CONCLUIDO` para qualquer outro status.

### RN-PROJ-04 — Exclusão
- Um projeto só pode ser excluído se estiver com status `EM_ORCAMENTO`.
- A exclusão é lógica (soft delete via campo `excluido_em`).
- A exclusão de um projeto cascateia para todos os seus ambientes, itens e gastos.

### RN-PROJ-05 — Cálculos
O sistema deve calcular e expor em tempo real:
- **Total orçado:** soma de `(quantidade × valor_unitario)` de todos os itens do projeto. Representa o teto macro do orçamento.
- **Total previsto:** soma de `valor_previsto` de todos os gastos do projeto. Representa o comprometimento operacional planejado.
- **Total efetivo:** soma de `valor_efetivo` dos gastos confirmados (onde `valor_efetivo IS NOT NULL`). Representa o que foi efetivamente pago.
- **Saldo restante:** total orçado − total efetivo.
- **Percentual efetivo:** (total efetivo / total orçado) × 100.
- **Percentual concluído:** média dos `percentual_concluido` de todos os ambientes.

### RN-PROJ-06 — Alerta de estouro
- Se `total efetivo > total orçado`, o projeto entra em estado de alerta visual.
- O sistema não bloqueia o lançamento de gastos acima do orçado, apenas sinaliza.

---

## Fluxos principais

### Fluxo 1 — Criar projeto
1. Usuário acessa a listagem de projetos.
2. Clica em "Novo projeto".
3. Preenche nome, tipo, endereço (opcional) e descrição (opcional).
4. Confirma. Sistema cria o projeto com status `EM_ORCAMENTO`.
5. Usuário é redirecionado para a tela de detalhes do projeto.

### Fluxo 2 — Iniciar obra
1. Usuário acessa os detalhes do projeto com status `EM_ORCAMENTO`.
2. Clica em "Iniciar obra".
3. Sistema valida que há ao menos um ambiente com um item.
4. Status muda para `EM_ANDAMENTO`.

### Fluxo 3 — Concluir obra
1. Usuário acessa os detalhes do projeto com status `EM_ANDAMENTO` ou `PAUSADO`.
2. Clica em "Concluir obra".
3. Sistema exibe confirmação com resumo financeiro (total orçado, total gasto, saldo).
4. Usuário confirma. Status muda para `CONCLUIDO`.

### Fluxo 4 — Excluir projeto
1. Usuário acessa a listagem ou detalhes do projeto com status `EM_ORCAMENTO`.
2. Clica em "Excluir".
3. Sistema exibe confirmação alertando que todos os dados serão removidos.
4. Usuário confirma. Soft delete aplicado.

---

## Validações

| Campo | Regra |
|---|---|
| `nome` | Obrigatório. Mínimo 3 caracteres. Máximo 200 caracteres. |
| `tipo` | Obrigatório. Valores aceitos: `CASA`, `APARTAMENTO`. |
| `endereco` | Opcional. Máximo 300 caracteres. |
| `descricao` | Opcional. Máximo 500 caracteres. |
| `status` | Gerenciado pelo sistema. Não enviado pelo cliente na criação. |

---

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

---

# Obra Certa — Spec de Entidades e Banco: Módulo Projetos

## Entidade: Projeto

### Tabela: `projeto`

| Coluna | Tipo PostgreSQL | Nullable | Default | Descrição |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | NOT NULL | auto | Chave primária |
| `nome` | `VARCHAR(200)` | NOT NULL | — | Nome do projeto |
| `tipo` | `VARCHAR(20)` | NOT NULL | — | `CASA` ou `APARTAMENTO` |
| `endereco` | `VARCHAR(300)` | NULL | — | Endereço do imóvel |
| `descricao` | `VARCHAR(500)` | NULL | — | Descrição livre |
| `status` | `VARCHAR(30)` | NOT NULL | `'EM_ORCAMENTO'` | Status atual |
| `criado_em` | `TIMESTAMP` | NOT NULL | `NOW()` | Data de criação |
| `atualizado_em` | `TIMESTAMP` | NOT NULL | `NOW()` | Última atualização |
| `excluido_em` | `TIMESTAMP` | NULL | — | Soft delete |

### Constraints

```sql
ALTER TABLE projeto
  ADD CONSTRAINT chk_projeto_tipo
    CHECK (tipo IN ('CASA', 'APARTAMENTO'));

ALTER TABLE projeto
  ADD CONSTRAINT chk_projeto_status
    CHECK (status IN ('EM_ORCAMENTO', 'EM_ANDAMENTO', 'PAUSADO', 'CONCLUIDO'));
```

### Índices

```sql
CREATE INDEX idx_projeto_status     ON projeto(status)     WHERE excluido_em IS NULL;
CREATE INDEX idx_projeto_tipo       ON projeto(tipo)       WHERE excluido_em IS NULL;
CREATE INDEX idx_projeto_criado_em  ON projeto(criado_em DESC);
CREATE INDEX idx_projeto_excluido   ON projeto(excluido_em) WHERE excluido_em IS NOT NULL;
```

### Migration Flyway

```sql
-- V1__create_projeto.sql

CREATE TABLE projeto (
  id           BIGSERIAL    PRIMARY KEY,
  nome         VARCHAR(200) NOT NULL,
  tipo         VARCHAR(20)  NOT NULL,
  endereco     VARCHAR(300),
  descricao    VARCHAR(500),
  status       VARCHAR(30)  NOT NULL DEFAULT 'EM_ORCAMENTO',
  criado_em    TIMESTAMP    NOT NULL DEFAULT NOW(),
  atualizado_em TIMESTAMP   NOT NULL DEFAULT NOW(),
  excluido_em  TIMESTAMP,

  CONSTRAINT chk_projeto_tipo   CHECK (tipo   IN ('CASA', 'APARTAMENTO')),
  CONSTRAINT chk_projeto_status CHECK (status IN ('EM_ORCAMENTO', 'EM_ANDAMENTO', 'PAUSADO', 'CONCLUIDO'))
);

CREATE INDEX idx_projeto_status    ON projeto(status)    WHERE excluido_em IS NULL;
CREATE INDEX idx_projeto_tipo      ON projeto(tipo)      WHERE excluido_em IS NULL;
CREATE INDEX idx_projeto_criado_em ON projeto(criado_em DESC);
```

---

## Entidade JPA: `Projeto.java`

```java
package com.obracerta.projeto.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projeto")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProjeto tipo;

    @Column(length = 300)
    private String endereco;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusProjeto status = StatusProjeto.EM_ORCAMENTO;

    @OneToMany(mappedBy = "projeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ambiente> ambientes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    @Column
    private LocalDateTime excluidoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public boolean isExcluido() {
        return excluidoEm != null;
    }

    public boolean isConcluido() {
        return status == StatusProjeto.CONCLUIDO;
    }
}
```

---

## Enums

```java
// TipoProjeto.java
package com.obracerta.projeto.domain;

public enum TipoProjeto {
    CASA,
    APARTAMENTO
}
```

```java
// StatusProjeto.java
package com.obracerta.projeto.domain;

public enum StatusProjeto {
    EM_ORCAMENTO,
    EM_ANDAMENTO,
    PAUSADO,
    CONCLUIDO;

    public boolean podeTransicionarPara(StatusProjeto destino) {
        return switch (this) {
            case EM_ORCAMENTO -> destino == EM_ANDAMENTO;
            case EM_ANDAMENTO -> destino == PAUSADO || destino == CONCLUIDO;
            case PAUSADO      -> destino == EM_ANDAMENTO;
            case CONCLUIDO    -> false;
        };
    }
}
```

---

## DTOs

```java
// ProjetoRequest.java
package com.obracerta.projeto.dto;

import com.obracerta.projeto.domain.TipoProjeto;
import jakarta.validation.constraints.*;

public record ProjetoRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 200, message = "Nome deve ter entre 3 e 200 caracteres")
    String nome,

    @NotNull(message = "Tipo é obrigatório")
    TipoProjeto tipo,

    @Size(max = 300, message = "Endereço deve ter no máximo 300 caracteres")
    String endereco,

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    String descricao
) {}
```

```java
// ProjetoResponse.java
package com.obracerta.projeto.dto;

import com.obracerta.projeto.domain.StatusProjeto;
import com.obracerta.projeto.domain.TipoProjeto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProjetoResponse(
    Long id,
    String nome,
    TipoProjeto tipo,
    String endereco,
    String descricao,
    StatusProjeto status,
    BigDecimal totalOrcado,
    BigDecimal totalPrevisto,
    BigDecimal totalEfetivo,
    BigDecimal saldoRestante,
    BigDecimal percentualEfetivo,
    BigDecimal percentualConcluido,
    boolean emAlerta,
    int totalAmbientes,
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm
) {}
```

```java
// StatusRequest.java
package com.obracerta.projeto.dto;

import com.obracerta.projeto.domain.StatusProjeto;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
    @NotNull(message = "Status é obrigatório")
    StatusProjeto status
) {}
```

---

## Repository

```java
package com.obracerta.projeto.repository;

import com.obracerta.projeto.domain.Projeto;
import com.obracerta.projeto.domain.StatusProjeto;
import com.obracerta.projeto.domain.TipoProjeto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {

    // Busca somente projetos não excluídos
    Page<Projeto> findByExcluidoEmIsNull(Pageable pageable);

    // Filtros combinados
    @Query("""
        SELECT p FROM Projeto p
        WHERE p.excluidoEm IS NULL
          AND (:status IS NULL OR p.status = :status)
          AND (:tipo IS NULL OR p.tipo = :tipo)
          AND (:busca IS NULL OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.endereco) LIKE LOWER(CONCAT('%', :busca, '%')))
        """)
    Page<Projeto> buscarComFiltros(
        @Param("status") StatusProjeto status,
        @Param("tipo") TipoProjeto tipo,
        @Param("busca") String busca,
        Pageable pageable
    );

    // Garante que o soft-deleted não apareça no findById
    Optional<Projeto> findByIdAndExcluidoEmIsNull(Long id);
}
```

---

# Obra Certa — Spec Angular: Módulo Projetos

## Estrutura de arquivos

```
src/app/features/projetos/
├── projetos.module.ts
├── projetos-routing.module.ts
├── pages/
│   ├── projetos-lista/
│   │   ├── projetos-lista.component.ts
│   │   ├── projetos-lista.component.html
│   │   └── projetos-lista.component.scss
│   ├── projeto-detalhe/
│   │   ├── projeto-detalhe.component.ts
│   │   ├── projeto-detalhe.component.html
│   │   └── projeto-detalhe.component.scss
│   └── projeto-form/
│       ├── projeto-form.component.ts
│       ├── projeto-form.component.html
│       └── projeto-form.component.scss
├── components/
│   ├── projeto-card/
│   │   ├── projeto-card.component.ts
│   │   └── projeto-card.component.html
│   └── projeto-status-badge/
│       ├── projeto-status-badge.component.ts
│       └── projeto-status-badge.component.html
└── services/
    └── projeto.service.ts
```

---

## Rotas

```typescript
// projetos-routing.module.ts
const routes: Routes = [
  { path: '',           component: ProjetosListaComponent },
  { path: 'novo',       component: ProjetoFormComponent },
  { path: ':id',        component: ProjetoDetalheComponent },
  { path: ':id/editar', component: ProjetoFormComponent },
];
```

---

## Models

```typescript
// src/app/core/models/projeto.model.ts

export type TipoProjeto = 'CASA' | 'APARTAMENTO';

export type StatusProjeto =
  | 'EM_ORCAMENTO'
  | 'EM_ANDAMENTO'
  | 'PAUSADO'
  | 'CONCLUIDO';

export interface Projeto {
  id: number;
  nome: string;
  tipo: TipoProjeto;
  endereco?: string;
  descricao?: string;
  status: StatusProjeto;
  totalOrcado: number;
  totalPrevisto: number;
  totalEfetivo: number;
  saldoRestante: number;
  percentualEfetivo: number;
  percentualConcluido: number;
  emAlerta: boolean;
  totalAmbientes: number;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ProjetoRequest {
  nome: string;
  tipo: TipoProjeto;
  endereco?: string;
  descricao?: string;
}

export interface ProjetoResumo {
  projetoId: number;
  nome: string;
  status: StatusProjeto;
  totais: {
    orcado: number;
    previsto: number;
    efetivo: number;
    saldo: number;
    percentualEfetivo: number;
    percentualConcluido: number;
    emAlerta: boolean;
  };
  porAmbiente: {
    ambienteId: number;
    nome: string;
    orcado: number;
    previsto: number;
    efetivo: number;
    saldo: number;
    percentualEfetivo: number;
    percentualConcluido: number;
    emAlerta: boolean;
  }[];
  ultimosGastos: {
    gastoId: number;
    descricao: string;
    valorPrevisto: number;
    valorEfetivo: number | null;
    dataPrevista: string;
    dataEfetiva: string | null;
    pendente: boolean;
    ambienteNome: string;
  }[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
```

---

## Service

```typescript
// projeto.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Projeto, ProjetoRequest, ProjetoResumo, PageResponse, StatusProjeto, TipoProjeto } from '@core/models/projeto.model';

export interface ProjetoFiltros {
  status?: StatusProjeto;
  tipo?: TipoProjeto;
  busca?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProjetoService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/projetos`;

  listar(filtros: ProjetoFiltros = {}): Observable<PageResponse<Projeto>> {
    let params = new HttpParams();
    if (filtros.status)  params = params.set('status',  filtros.status);
    if (filtros.tipo)    params = params.set('tipo',    filtros.tipo);
    if (filtros.busca)   params = params.set('busca',   filtros.busca);
    if (filtros.page != null) params = params.set('page', filtros.page);
    if (filtros.size != null) params = params.set('size', filtros.size);
    if (filtros.sort)    params = params.set('sort',    filtros.sort);
    return this.http.get<PageResponse<Projeto>>(this.baseUrl, { params });
  }

  buscarPorId(id: number): Observable<Projeto> {
    return this.http.get<Projeto>(`${this.baseUrl}/${id}`);
  }

  criar(dados: ProjetoRequest): Observable<Projeto> {
    return this.http.post<Projeto>(this.baseUrl, dados);
  }

  atualizar(id: number, dados: ProjetoRequest): Observable<Projeto> {
    return this.http.put<Projeto>(`${this.baseUrl}/${id}`, dados);
  }

  atualizarStatus(id: number, status: StatusProjeto): Observable<Projeto> {
    return this.http.patch<Projeto>(`${this.baseUrl}/${id}/status`, { status });
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  resumo(id: number): Observable<ProjetoResumo> {
    return this.http.get<ProjetoResumo>(`${this.baseUrl}/${id}/resumo`);
  }
}
```

---

## Tela 1 — Lista de Projetos (`projetos-lista`)

### Comportamento
- Exibe cards de todos os projetos, paginados (20 por página).
- Filtros no topo: busca por texto, filtro de status, filtro de tipo.
- Botão "Novo projeto" no canto superior direito.
- Estado vazio: ilustração + mensagem "Nenhum projeto encontrado. Crie seu primeiro projeto."
- Estado de carregamento: skeleton loader nos cards.
- Ao clicar no card, navega para `/projetos/:id`.

### Campos exibidos no card (`projeto-card`)
- Nome do projeto
- Badge de status (componente `projeto-status-badge`)
- Tipo: ícone + label (casa / apartamento)
- Endereço (se preenchido)
- Barra de progresso: percentual concluído
- Valores: total orçado e total gasto
- Indicador de alerta (ícone vermelho) se `emAlerta = true`
- Total de ambientes

### Filtros
| Filtro | Componente Angular Material | Comportamento |
|---|---|---|
| Busca | `mat-form-field` + `matInput` | Debounce 400ms, busca ao digitar |
| Status | `mat-select` | Seleção única, opcional |
| Tipo | `mat-button-toggle-group` | Todos / Casa / Apartamento |

---

## Tela 2 — Formulário de Projeto (`projeto-form`)

### Comportamento
- Modo criação: título "Novo projeto", botão "Criar projeto".
- Modo edição: título "Editar projeto", pré-carrega dados, botão "Salvar alterações".
- Ao salvar com sucesso, redireciona para `/projetos/:id` com snackbar de confirmação.
- Ao cancelar, volta para a tela anterior.
- Campos desabilitados se projeto estiver com status `CONCLUIDO`.

### Formulário (Reactive Forms)

| Campo | Componente | Validação | Observações |
|---|---|---|---|
| Nome | `matInput` text | Obrigatório, min 3, max 200 | Foco automático ao abrir |
| Tipo | `mat-radio-group` | Obrigatório | Opções: Casa / Apartamento |
| Endereço | `matInput` text | Máx 300 | Opcional |
| Descrição | `matInput` textarea | Máx 500 | Opcional, 3 linhas |

### Mensagens de erro inline
- Nome vazio: "Nome é obrigatório"
- Nome curto: "Nome deve ter no mínimo 3 caracteres"
- Tipo não selecionado: "Selecione o tipo do imóvel"

---

## Tela 3 — Detalhe do Projeto (`projeto-detalhe`)

### Comportamento
- Cabeçalho com nome, badge de status e botões de ação contextuais.
- Aba ou seção de resumo financeiro (cards de totais).
- Listagem de ambientes do projeto (carregada do módulo de ambientes).
- Menu de ações: Editar, Mudar status, Excluir.

### Botões de ação por status

| Status atual | Ações disponíveis |
|---|---|
| `EM_ORCAMENTO` | Editar · Iniciar obra · Excluir |
| `EM_ANDAMENTO` | Editar · Pausar obra · Concluir obra |
| `PAUSADO` | Editar · Retomar obra · Concluir obra |
| `CONCLUIDO` | (somente visualização) |

### Cards de resumo financeiro
- Total orçado
- Total gasto
- Saldo restante (verde se positivo, vermelho se negativo)
- Percentual concluído (com `mat-progress-bar`)

### Confirmações obrigatórias
- "Excluir projeto": dialog com aviso de que todos os dados serão perdidos.
- "Concluir obra": dialog com resumo financeiro antes de confirmar.

---

## Componente: `projeto-status-badge`

Badge visual com cor por status:

| Status | Cor de fundo | Cor do texto | Label |
|---|---|---|---|
| `EM_ORCAMENTO` | Azul claro | Azul escuro | Em orçamento |
| `EM_ANDAMENTO` | Verde claro | Verde escuro | Em andamento |
| `PAUSADO` | Amarelo claro | Amarelo escuro | Pausado |
| `CONCLUIDO` | Cinza claro | Cinza escuro | Concluído |

```typescript
// projeto-status-badge.component.ts
@Component({
  selector: 'app-projeto-status-badge',
  template: `<span [class]="'badge badge--' + status.toLowerCase()">{{ label }}</span>`,
})
export class ProjetoStatusBadgeComponent {
  @Input({ required: true }) status!: StatusProjeto;

  get label(): string {
    const labels: Record<StatusProjeto, string> = {
      EM_ORCAMENTO: 'Em orçamento',
      EM_ANDAMENTO: 'Em andamento',
      PAUSADO: 'Pausado',
      CONCLUIDO: 'Concluído',
    };
    return labels[this.status];
  }
}
```

---

## Observações para o Claude Code

1. Usar **Signals** (`signal`, `computed`, `effect`) nos components em vez de `BehaviorSubject` onde possível.
2. Usar `inject()` no lugar de injeção por construtor.
3. Tratamento de erros centralizado via `HttpInterceptor` — exibir snackbar com a mensagem do backend.
4. O `ProjetoService` deve ser o único ponto de acesso à API — components não fazem chamadas HTTP diretamente.
5. Paginação com `MatPaginator` integrado ao endpoint paginado do backend.
6. Usar `trackBy` em todos os `*ngFor` para otimizar renderização.
7. Preferir **Standalone Components** (Angular 17+) ao padrão NgModule — refatorar o módulo `projetos` para standalone assim que os demais módulos forem criados.
