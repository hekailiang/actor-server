CREATE TABLE public_keys (
       user_id int NOT NULL,
       hash bigint NOT NULL,
       data bytea NOT NULL,
       auth_id bigint NOT NULL,
       deleted_at timestamp,
       PRIMARY KEY (user_id, hash)
);
