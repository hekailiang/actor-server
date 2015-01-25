CREATE TABLE apple_push_credentials (
       auth_id bigint NOT NULL,
       apns_key int NOT NULL,
       token varchar(128) NOT NULL,
       PRIMARY KEY (auth_id)
);

CREATE INDEX on apple_push_credentials(token);
