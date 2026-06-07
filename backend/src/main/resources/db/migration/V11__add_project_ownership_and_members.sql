ALTER TABLE project ADD COLUMN owner_id BIGINT REFERENCES app_user(id);

CREATE TABLE project_member (
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role       VARCHAR(10) NOT NULL CHECK (role IN ('VIEWER', 'EDITOR')),
    PRIMARY KEY (project_id, user_id)
);

CREATE INDEX idx_project_member_user ON project_member(user_id);
