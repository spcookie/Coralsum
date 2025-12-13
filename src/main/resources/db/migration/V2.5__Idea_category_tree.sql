ALTER TABLE idea_category
    ADD COLUMN parent_id BIGINT;
ALTER TABLE idea_category
    ADD COLUMN order_no INT;

CREATE INDEX IF NOT EXISTS idx_idea_category_parent ON idea_category (parent_id);
