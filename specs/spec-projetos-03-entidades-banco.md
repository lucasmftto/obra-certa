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
