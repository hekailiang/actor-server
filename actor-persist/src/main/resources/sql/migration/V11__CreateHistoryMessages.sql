CREATE TABLE history_messages (
       user_id int NOT NULL,
       peer_type int NOT NULL,
       peer_id int NOT NULL,
       sender_user_id int NOT NULL,
       date timestamp NOT NULL,
       random_id bigint NOT NULL,
       message_content_header int NOT NULL,
       message_content_data bytea NOT NULL,
       is_deleted boolean NOT NULL default false,
       PRIMARY KEY(user_id, peer_type, peer_id, sender_user_id, date, random_id)
);

CREATE INDEX ON history_messages(user_id, peer_type, peer_id, random_id);
