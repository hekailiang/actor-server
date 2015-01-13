CREATE TABLE avatar_datas (
       entity_id bigint NOT NULL,
       entity_kind int NOT NULL,
       small_avatar_file_id int,
       small_avatar_file_hash bigint,
       small_avatar_file_size int,
       large_avatar_file_id int,
       large_avatar_file_hash bigint,
       large_avatar_file_size int,
       full_avatar_file_id int,
       full_avatar_file_hash bigint,
       full_avatar_file_size int,
       full_avatar_width int,
       full_avatar_height int,
       PRIMARY KEY (entity_id, entity_kind)
);
