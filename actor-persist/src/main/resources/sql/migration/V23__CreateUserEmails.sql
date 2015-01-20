CREATE TABLE user_emails (
       id int NOT NULL,
       user_id int NOT NULL,
       email varchar(255) NOT NULL,
       access_salt varchar(255) NOT NULL,
       title varchar(255) NOT NULL,
       PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ON user_emails (lower(email));
CREATE INDEX ON user_emails (user_id);
