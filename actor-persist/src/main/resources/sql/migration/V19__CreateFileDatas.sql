CREATE TABLE file_datas (
       id bigint NOT NULL,
       access_salt varchar(255) NOT NULL,
       uploaded_blocks_count int NOT NULL default 0,
       length bigint NOT NULL default 0,
       PRIMARY KEY (id)
);
