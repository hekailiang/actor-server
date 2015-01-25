ALTER TABLE history_messages ADD COLUMN is_read boolean NOT NULL default false;

CREATE INDEX on history_messages (user_id, peer_type, peer_id, is_read);

CREATE TABLE dialogs (
       user_id int NOT NULL,
       peer_type int NOT NULL,
       peer_id int NOT NULL,
       sender_user_id int NOT NULL,
       date timestamp NOT NULL,
       random_id bigint NOT NULL,
       message_content_header int NOT NULL,
       message_content_data bytea NOT NULL,
       PRIMARY KEY(user_id, peer_type, peer_id)
);
