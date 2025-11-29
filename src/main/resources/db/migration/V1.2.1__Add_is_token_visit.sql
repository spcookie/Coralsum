ALTER TABLE retrieval_image_req_record
    ADD COLUMN is_token_visit BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_retrieval_image_ref_token_visit
    ON retrieval_image_req_record (image_ref, is_token_visit);
