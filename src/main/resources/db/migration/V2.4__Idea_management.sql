CREATE TABLE IF NOT EXISTS idea_category
(
    id          IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   VARCHAR(100),
    update_by   VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS idea_tag
(
    id          IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   VARCHAR(100),
    update_by   VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS idea_template
(
    id                    IDENTITY PRIMARY KEY,
    name                  VARCHAR(200) NOT NULL,
    description           VARCHAR(1000),
    prompt_text           CLOB         NOT NULL,
    category_id           BIGINT,
    tips_need_image       BOOLEAN DEFAULT FALSE,
    tips_need_pro_model   BOOLEAN DEFAULT FALSE,
    tips_need_edit_prompt BOOLEAN DEFAULT FALSE,
    original_image_refs   CLOB,
    effect_image_ref      VARCHAR(1024),
    usage_count           INT     DEFAULT 0,
    last_used_time        TIMESTAMP,
    create_time           TIMESTAMP,
    update_time           TIMESTAMP,
    create_by             VARCHAR(100),
    update_by             VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS idea_template_tag_rel
(
    id          IDENTITY PRIMARY KEY,
    template_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    create_by   VARCHAR(100),
    update_by   VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_idea_template_category ON idea_template (category_id);
CREATE INDEX IF NOT EXISTS idx_idea_template_usage ON idea_template (usage_count, last_used_time);
CREATE INDEX IF NOT EXISTS idx_idea_template_tag_rel_template ON idea_template_tag_rel (template_id);
CREATE INDEX IF NOT EXISTS idx_idea_template_tag_rel_tag ON idea_template_tag_rel (tag_id);

