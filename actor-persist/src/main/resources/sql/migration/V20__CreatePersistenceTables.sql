CREATE TABLE IF NOT EXISTS akka_journal (
       persistence_id VARCHAR(255) NOT NULL,
       sequence_nr BIGINT NOT NULL,
       marker VARCHAR(255) NOT NULL,
       message BYTEA NOT NULL,
       created_at TIMESTAMP NOT NULL,
       PRIMARY KEY (persistence_id, sequence_nr)
);


CREATE TABLE IF NOT EXISTS akka_snapshot (
       persistence_id VARCHAR(255) NOT NULL,
       sequence_nr BIGINT NOT NULL,
       created_at BIGINT NOT NULL,
       snapshot BYTEA NOT NULL,
       PRIMARY KEY (persistence_id, sequence_nr)
);
