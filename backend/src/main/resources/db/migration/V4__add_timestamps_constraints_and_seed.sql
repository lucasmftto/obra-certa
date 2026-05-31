-- timestamps em item
ALTER TABLE item ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE item ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- updated_at em expense
ALTER TABLE expense ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- unique: nome do ambiente por projeto
ALTER TABLE environment
    ADD CONSTRAINT uq_environment_name_per_project
    UNIQUE (project_id, name);

-- seed categories
INSERT INTO category (name, color) VALUES
    ('Materials',  '#2196F3'),
    ('Labor',      '#4CAF50'),
    ('Transport',  '#FF9800'),
    ('Tools',      '#9C27B0'),
    ('Fees',       '#F44336'),
    ('Other',      '#607D8B');
