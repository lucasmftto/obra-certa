-- Rename TipoProjeto enum values
ALTER TABLE projeto DROP CONSTRAINT chk_projeto_tipo;
UPDATE projeto SET tipo = 'HOUSE'     WHERE tipo = 'CASA';
UPDATE projeto SET tipo = 'APARTMENT' WHERE tipo = 'APARTAMENTO';
ALTER TABLE projeto ADD CONSTRAINT chk_projeto_tipo
    CHECK (tipo IN ('HOUSE', 'APARTMENT'));

-- Rename StatusProjeto enum values
ALTER TABLE projeto DROP CONSTRAINT chk_projeto_status;
UPDATE projeto SET status = 'QUOTING'     WHERE status = 'EM_ORCAMENTO';
UPDATE projeto SET status = 'IN_PROGRESS' WHERE status = 'EM_ANDAMENTO';
UPDATE projeto SET status = 'PAUSED'      WHERE status = 'PAUSADO';
UPDATE projeto SET status = 'COMPLETED'   WHERE status = 'CONCLUIDO';
ALTER TABLE projeto ADD CONSTRAINT chk_projeto_status
    CHECK (status IN ('QUOTING', 'IN_PROGRESS', 'PAUSED', 'COMPLETED'));

-- Rename Item tipo enum value (preemptivo para quando Item for implementado)
ALTER TABLE item DROP CONSTRAINT chk_item_tipo;
UPDATE item SET tipo = 'LABOR' WHERE tipo = 'MAO_DE_OBRA';
ALTER TABLE item ADD CONSTRAINT chk_item_tipo
    CHECK (tipo IN ('MATERIAL', 'LABOR'));
