CREATE EXTENSION "uuid-ossp";

CREATE TABLE seq_updates (
       auth_id bigint NOT NULL,
       uuid uuid NOT NULL,
       header int NOT NULL,
       protobuf_data bytea NOT NULL,
       PRIMARY KEY (auth_id, uuid)
);
