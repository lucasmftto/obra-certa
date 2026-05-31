CREATE TABLE categoria (
  id    BIGSERIAL    PRIMARY KEY,
  nome  VARCHAR(100) NOT NULL,
  cor   VARCHAR(7)
);

CREATE TABLE projeto (
  id            BIGSERIAL    PRIMARY KEY,
  nome          VARCHAR(200) NOT NULL,
  tipo          VARCHAR(20)  NOT NULL,
  endereco      VARCHAR(300),
  descricao     VARCHAR(500),
  status        VARCHAR(30)  NOT NULL DEFAULT 'EM_ORCAMENTO',
  criado_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
  atualizado_em TIMESTAMP    NOT NULL DEFAULT NOW(),
  excluido_em   TIMESTAMP,

  CONSTRAINT chk_projeto_tipo   CHECK (tipo   IN ('CASA', 'APARTAMENTO')),
  CONSTRAINT chk_projeto_status CHECK (status IN ('EM_ORCAMENTO', 'EM_ANDAMENTO', 'PAUSADO', 'CONCLUIDO'))
);

CREATE INDEX idx_projeto_status    ON projeto(status)    WHERE excluido_em IS NULL;
CREATE INDEX idx_projeto_tipo      ON projeto(tipo)      WHERE excluido_em IS NULL;
CREATE INDEX idx_projeto_criado_em ON projeto(criado_em DESC);

CREATE TABLE ambiente (
  id                   BIGSERIAL    PRIMARY KEY,
  projeto_id           BIGINT       NOT NULL REFERENCES projeto(id) ON DELETE CASCADE,
  nome                 VARCHAR(150) NOT NULL,
  descricao            TEXT,
  ordem                INT          NOT NULL DEFAULT 0,
  percentual_concluido NUMERIC(5,2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_ambiente_projeto ON ambiente(projeto_id);

CREATE TABLE item (
  id             BIGSERIAL     PRIMARY KEY,
  ambiente_id    BIGINT        NOT NULL REFERENCES ambiente(id) ON DELETE CASCADE,
  descricao      VARCHAR(300)  NOT NULL,
  quantidade     NUMERIC(10,3) NOT NULL,
  unidade        VARCHAR(30)   NOT NULL,
  valor_unitario NUMERIC(12,2) NOT NULL,
  tipo           VARCHAR(20)   NOT NULL,

  CONSTRAINT chk_item_tipo CHECK (tipo IN ('MATERIAL', 'MAO_DE_OBRA'))
);

CREATE INDEX idx_item_ambiente ON item(ambiente_id);

CREATE TABLE gasto (
  id              BIGSERIAL     PRIMARY KEY,
  item_id         BIGINT        NOT NULL REFERENCES item(id) ON DELETE CASCADE,
  categoria_id    BIGINT        REFERENCES categoria(id),
  valor_previsto  NUMERIC(12,2) NOT NULL,
  valor_efetivo   NUMERIC(12,2),
  data_prevista   DATE          NOT NULL,
  data_efetiva    DATE,
  descricao       VARCHAR(300),
  comprovante     VARCHAR(500),
  criado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gasto_item      ON gasto(item_id);
CREATE INDEX idx_gasto_categoria ON gasto(categoria_id);
CREATE INDEX idx_gasto_data      ON gasto(data_prevista DESC);
