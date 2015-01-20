CREATE TABLE auth_sessions (
       user_id int NOT NULL,
       id int NOT NULL,
       app_id int NOT NULL,
       app_title varchar(64) NOT NULL,
       auth_id bigint NOT NULL,
       public_key_hash bigint NOT NULL,
       device_hash bytea NOT NULL,
       device_title varchar(64) NOT NULL,
       auth_time timestamp NOT NULL,
       auth_location varchar(255),
       latitude double precision,
       longitude double precision,
       deleted_at timestamp,
       PRIMARY KEY (user_id, id)
);
