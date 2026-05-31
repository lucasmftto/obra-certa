-- Drop FK constraints
ALTER TABLE ambiente DROP CONSTRAINT IF EXISTS ambiente_projeto_id_fkey;
ALTER TABLE item     DROP CONSTRAINT IF EXISTS item_ambiente_id_fkey;
ALTER TABLE gasto    DROP CONSTRAINT IF EXISTS gasto_item_id_fkey;
ALTER TABLE gasto    DROP CONSTRAINT IF EXISTS gasto_categoria_id_fkey;

-- Drop indexes
DROP INDEX IF EXISTS idx_projeto_status;
DROP INDEX IF EXISTS idx_projeto_tipo;
DROP INDEX IF EXISTS idx_projeto_criado_em;
DROP INDEX IF EXISTS idx_ambiente_projeto;
DROP INDEX IF EXISTS idx_item_ambiente;
DROP INDEX IF EXISTS idx_gasto_item;
DROP INDEX IF EXISTS idx_gasto_categoria;
DROP INDEX IF EXISTS idx_gasto_data;

-- Rename tables
ALTER TABLE projeto   RENAME TO project;
ALTER TABLE ambiente  RENAME TO environment;
ALTER TABLE gasto     RENAME TO expense;
ALTER TABLE categoria RENAME TO category;

-- Rename project columns
ALTER TABLE project RENAME COLUMN nome         TO name;
ALTER TABLE project RENAME COLUMN tipo         TO type;
ALTER TABLE project RENAME COLUMN endereco     TO address;
ALTER TABLE project RENAME COLUMN descricao    TO description;
ALTER TABLE project RENAME COLUMN criado_em    TO created_at;
ALTER TABLE project RENAME COLUMN atualizado_em TO updated_at;
ALTER TABLE project RENAME COLUMN excluido_em  TO deleted_at;

-- Update project status enum (QUOTING->IN_BUDGET, PAUSED->ON_HOLD)
ALTER TABLE project DROP CONSTRAINT IF EXISTS chk_projeto_status;
UPDATE project SET status = 'IN_BUDGET' WHERE status = 'QUOTING';
UPDATE project SET status = 'ON_HOLD'   WHERE status = 'PAUSED';
ALTER TABLE project ADD CONSTRAINT chk_project_status
    CHECK (status IN ('IN_BUDGET', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED'));

-- Update project type enum constraint name
ALTER TABLE project DROP CONSTRAINT IF EXISTS chk_projeto_tipo;
ALTER TABLE project ADD CONSTRAINT chk_project_type
    CHECK (type IN ('HOUSE', 'APARTMENT'));

-- Rename environment columns
ALTER TABLE environment RENAME COLUMN projeto_id            TO project_id;
ALTER TABLE environment RENAME COLUMN nome                  TO name;
ALTER TABLE environment RENAME COLUMN descricao             TO description;
ALTER TABLE environment RENAME COLUMN ordem                 TO sort_order;
ALTER TABLE environment RENAME COLUMN percentual_concluido  TO completion_percentage;

-- Rename item columns
ALTER TABLE item RENAME COLUMN ambiente_id    TO environment_id;
ALTER TABLE item RENAME COLUMN descricao      TO description;
ALTER TABLE item RENAME COLUMN quantidade     TO quantity;
ALTER TABLE item RENAME COLUMN unidade        TO unit;
ALTER TABLE item RENAME COLUMN valor_unitario TO unit_price;
ALTER TABLE item RENAME COLUMN tipo           TO type;
ALTER TABLE item DROP CONSTRAINT IF EXISTS chk_item_tipo;
ALTER TABLE item ADD CONSTRAINT chk_item_type
    CHECK (type IN ('MATERIAL', 'LABOR'));

-- Rename expense columns
ALTER TABLE expense RENAME COLUMN categoria_id    TO category_id;
ALTER TABLE expense RENAME COLUMN valor_previsto  TO planned_amount;
ALTER TABLE expense RENAME COLUMN valor_efetivo   TO actual_amount;
ALTER TABLE expense RENAME COLUMN data_prevista   TO planned_date;
ALTER TABLE expense RENAME COLUMN data_efetiva    TO actual_date;
ALTER TABLE expense RENAME COLUMN descricao       TO description;
ALTER TABLE expense RENAME COLUMN comprovante     TO receipt_url;
ALTER TABLE expense RENAME COLUMN criado_em       TO created_at;

-- Rename category columns
ALTER TABLE category RENAME COLUMN nome TO name;
ALTER TABLE category RENAME COLUMN cor  TO color;

-- Re-add FK constraints
ALTER TABLE environment ADD CONSTRAINT fk_environment_project
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE;
ALTER TABLE item ADD CONSTRAINT fk_item_environment
    FOREIGN KEY (environment_id) REFERENCES environment(id) ON DELETE CASCADE;
ALTER TABLE expense ADD CONSTRAINT fk_expense_item
    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE;
ALTER TABLE expense ADD CONSTRAINT fk_expense_category
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;

-- Re-create indexes
CREATE INDEX idx_project_status      ON project(status)      WHERE deleted_at IS NULL;
CREATE INDEX idx_project_type        ON project(type)        WHERE deleted_at IS NULL;
CREATE INDEX idx_project_created_at  ON project(created_at DESC);
CREATE INDEX idx_environment_project ON environment(project_id);
CREATE INDEX idx_item_environment    ON item(environment_id);
CREATE INDEX idx_expense_item        ON expense(item_id);
CREATE INDEX idx_expense_category    ON expense(category_id);
CREATE INDEX idx_expense_date        ON expense(planned_date DESC);
