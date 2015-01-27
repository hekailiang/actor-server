CREATE TABLE file_blocks (
       file_id bigint NOT NULL,
       offset_ bigint NOT NULL,
       length bigint NOT NULL,
       PRIMARY KEY (file_id, offset_, length)
);
