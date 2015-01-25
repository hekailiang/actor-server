CREATE TABLE avatar_datas (
       entity_id bigint NOT NULL,
       entity_type int NOT NULL,
       small_avatar_file_id bigint,
       small_avatar_file_hash bigint,
       small_avatar_file_size int,
       large_avatar_file_id bigint,
       large_avatar_file_hash bigint,
       large_avatar_file_size int,
       full_avatar_file_id bigint,
       full_avatar_file_hash bigint,
       full_avatar_file_size int,
       full_avatar_width int,
       full_avatar_height int,
       PRIMARY KEY (entity_id, entity_type)
);
