# Obra Certa — Planejamento do Sistema

## Visão Geral

Sistema para gerenciamento de orçamentos e controle de gastos em reformas de casas e apartamentos. A estrutura hierárquica é: **Projeto → Ambientes → Itens → Gastos**.

---

## Stack Técnica

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21 + Spring Boot 3 |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | PostgreSQL 16 |
| Migrations | Flyway |
| Mapeamento | MapStruct |
| Validação | Bean Validation (Jakarta) |
| Segurança | Spring Security |
| Frontend | Angular 17+ |
| UI Components | Angular Material |
| HTTP | HttpClient (Angular) |
| Gráficos | Chart.js ou ngx-charts |

---

## Modelo de Dados

```
Projeto (1) ──→ (N) Ambiente (1) ──→ (N) Item (1) ──→ (N) Gasto
```

### Projeto
- id, nome, endereço, tipo (CASA / APARTAMENTO), status, dataCriacao

### Ambiente
- id, nome, descrição, ordem, percentualConcluido, projeto_id

### Item
- id, descrição, quantidade, unidade, valorUnitario, tipo (MATERIAL / MAO_DE_OBRA), ambiente_id

### Gasto
- id, valorPrevisto, valorEfetivo (nullable), dataPrevista, dataEfetiva (nullable), descrição, comprovante (URL/path), item_id, categoria_id

### Categoria
- id, nome, cor (hex)

---

## Estrutura de Pacotes — Backend

```
com.obracerta
├── project
│   ├── controller    (ProjetoController.java)
│   ├── service       (ProjetoService.java)
│   ├── repository    (ProjetoRepository.java)
│   ├── domain        (Projeto.java)
│   └── dto           (ProjetoRequest.java, ProjetoResponse.java)
├── ambiente
│   ├── controller, service, repository
│   ├── domain        (Ambiente.java)
│   └── dto
├── item
│   ├── controller, service, repository
│   ├── domain        (Item.java)
│   └── dto
├── gasto
│   ├── controller, service, repository
│   ├── domain        (Gasto.java)
│   └── dto
├── categoria
│   ├── controller, service, repository
│   ├── domain        (Categoria.java)
│   └── dto
└── shared
    ├── exception     (GlobalExceptionHandler.java)
    ├── dto           (ApiResponse.java, PageResponse.java)
    └── config        (SecurityConfig.java, CorsConfig.java)
```

---

## Estrutura de Módulos — Frontend Angular

```
src/app
├── core
│   ├── services      (api.service.ts, auth.service.ts)
│   ├── interceptors  (auth.interceptor.ts)
│   └── models        (projeto.model.ts, ambiente.model.ts, ...)
├── shared
│   ├── components    (breadcrumb, confirm-dialog, loading...)
│   └── pipes
├── features
│   ├── projetos      (listagem, formulário, detalhes)
│   ├── ambientes     (listagem por projeto, formulário)
│   ├── itens         (tabela por ambiente, inline edit)
│   ├── gastos        (lançamento, listagem, filtros)
│   └── dashboard     (cards, gráficos, alertas)
└── layout
    ├── sidebar
    └── topbar
```

---

## Etapas de Implementação

### Etapa 1 — Setup e Infraestrutura
- Criar projeto Spring Boot com dependências (JPA, Security, Flyway, Validation, MapStruct, PostgreSQL Driver)
- Configurar `application.yml` com datasource PostgreSQL
- Criar primeira migration Flyway com o schema completo (`V1__create_schema.sql`)
- Criar projeto Angular com Angular Material e estrutura de módulos
- Configurar CORS no backend para aceitar requisições do Angular

### Etapa 2 — Backend: Projetos e Ambientes
- Entidades `Projeto` e `Ambiente` com mapeamento JPA (`@OneToMany` / `@ManyToOne`)
- Repositories (Spring Data JPA), Services e Controllers REST
- DTOs com MapStruct + validações (`@NotBlank`, `@NotNull`, `@Positive`)
- Endpoints CRUD completos:
  - `GET/POST /api/v1/projetos`
  - `GET/PUT/DELETE /api/v1/projetos/{id}`
  - `GET/POST /api/v1/projetos/{id}/ambientes`
  - `GET/PUT/DELETE /api/v1/ambientes/{id}`

### Etapa 3 — Backend: Itens e Gastos
- Entidades `Item` (vinculado a Ambiente), `Gasto` (vinculado a Item) e `Categoria`
- Cálculos agregados por ambiente: total orçado, total gasto, saldo restante
- Endpoint de resumo por projeto (alimenta o dashboard):
  - `GET /api/v1/projetos/{id}/resumo`
- Endpoints:
  - `GET/POST /api/v1/ambientes/{id}/itens`
  - `GET/PUT/DELETE /api/v1/itens/{id}`
  - `GET/POST /api/v1/itens/{id}/gastos`
  - `GET/PUT/DELETE /api/v1/gastos/{id}`
  - `GET/POST /api/v1/categorias`

### Etapa 4 — Frontend: Projetos e Ambientes
- Módulo `projetos`: listagem com cards de status, formulário de criação/edição
- Módulo `ambientes`: listagem por projeto, formulário de criação/edição
- Módulo `itens`: tabela de itens por ambiente com inline edit
- Services Angular com HttpClient consumindo todos os endpoints da API

### Etapa 5 — Frontend: Lançamento de Gastos
- Formulário de lançamento de gasto vinculado a um item
- Listagem de gastos com filtros por data e categoria
- Indicador visual (badge/chip) de item dentro ou fora do orçado
- Upload opcional de comprovante (nota fiscal)

### Etapa 6 — Dashboard
- Cards de resumo: total orçado, total gasto, saldo restante, % concluído geral
- Gráfico de barras agrupadas por ambiente (previsto vs realizado)
- Barra de progresso individual por ambiente
- Lista dos últimos gastos lançados
- Alertas visuais de estouro de orçamento por item/ambiente

### Etapa 7 — Polimentos e Qualidade
- Responsividade e ajustes de UX mobile
- Exportação PDF do orçamento e do comparativo previsto/realizado
- Paginação e filtros nas listagens
- Testes unitários nos Services do backend
- Tratamento global de erros (frontend e backend)

---

## Dependências — pom.xml (principais)

```xml
<dependencies>
  <!-- Spring Boot -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>

  <!-- PostgreSQL -->
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>

  <!-- Flyway (PostgreSQL built into flyway-core no Flyway 9.x gerenciado pelo Spring Boot 3.2.x) -->
  <dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
  </dependency>

  <!-- MapStruct -->
  <dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
  </dependency>
  <dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
  </dependency>

  <!-- Lombok -->
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
  </dependency>

  <!-- Test -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

---

## application.yml (base)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/obra_certa_db
    username: obra_certa_user
    password: obra_certa_pass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080
```

---

## Migration inicial — V1__create_schema.sql (esboço)

```sql
CREATE TABLE categoria (
  id         BIGSERIAL PRIMARY KEY,
  nome       VARCHAR(100) NOT NULL,
  cor        VARCHAR(7)
);

CREATE TABLE projeto (
  id          BIGSERIAL PRIMARY KEY,
  nome        VARCHAR(200) NOT NULL,
  endereco    VARCHAR(300),
  tipo        VARCHAR(20) NOT NULL CHECK (tipo IN ('CASA', 'APARTAMENTO')),
  status      VARCHAR(30) NOT NULL DEFAULT 'EM_ORCAMENTO',
  criado_em   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE ambiente (
  id                   BIGSERIAL PRIMARY KEY,
  projeto_id           BIGINT NOT NULL REFERENCES projeto(id) ON DELETE CASCADE,
  nome                 VARCHAR(150) NOT NULL,
  descricao            TEXT,
  ordem                INT NOT NULL DEFAULT 0,
  percentual_concluido NUMERIC(5,2) NOT NULL DEFAULT 0
);

CREATE TABLE item (
  id             BIGSERIAL PRIMARY KEY,
  ambiente_id    BIGINT NOT NULL REFERENCES ambiente(id) ON DELETE CASCADE,
  descricao      VARCHAR(300) NOT NULL,
  quantidade     NUMERIC(10,3) NOT NULL,
  unidade        VARCHAR(30) NOT NULL,
  valor_unitario NUMERIC(12,2) NOT NULL,
  tipo           VARCHAR(20) NOT NULL CHECK (tipo IN ('MATERIAL', 'MAO_DE_OBRA'))
);

CREATE TABLE gasto (
  id              BIGSERIAL PRIMARY KEY,
  item_id         BIGINT NOT NULL REFERENCES item(id) ON DELETE CASCADE,
  categoria_id    BIGINT REFERENCES categoria(id),
  valor_previsto  NUMERIC(12,2) NOT NULL,
  valor_efetivo   NUMERIC(12,2),
  data_prevista   DATE NOT NULL,
  data_efetiva    DATE,
  descricao       VARCHAR(300),
  comprovante     VARCHAR(500),
  criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## Dependências Angular (package.json — principais)

```json
"dependencies": {
  "@angular/animations": "^17.0.0",
  "@angular/cdk": "^17.0.0",
  "@angular/common": "^17.0.0",
  "@angular/core": "^17.0.0",
  "@angular/forms": "^17.0.0",
  "@angular/material": "^17.0.0",
  "@angular/router": "^17.0.0",
  "chart.js": "^4.4.0",
  "ng2-charts": "^5.0.0",
  "rxjs": "~7.8.0"
}
```

---

## Observações para o Claude Code

1. Implementar **uma etapa por vez**, validando o funcionamento antes de avançar.
2. Sempre criar os testes unitários dos Services junto com a implementação.
3. Usar **Records Java** para DTOs de request/response onde possível.
4. No Angular, usar **Signals** (Angular 17+) nos components sempre que possível em vez de Observables puros.
5. O endpoint `/api/projetos/{id}/resumo` é crítico — ele agrega todos os cálculos e alimenta o dashboard; implementar com query JPQL nativa para performance.
6. Configurar CORS no `SecurityConfig` para `http://localhost:4200` durante desenvolvimento.
