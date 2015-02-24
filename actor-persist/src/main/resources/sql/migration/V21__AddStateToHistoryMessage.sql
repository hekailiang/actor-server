ALTER TABLE history_messages ADD COLUMN state int NOT NULL default 1;

DROP INDEX idx_history_messages_user_peer_is_read;
ALTER TABLE history_messages DROP COLUMN is_read;

ALTER TABLE dialogs ADD COLUMN state int;

CREATE INDEX idx_history_messages_user_peer_sender_state ON history_messages(user_id, peer_type, peer_id, sender_user_id, state);
