CREATE TABLE google_push_credentials (
       auth_id bigint NOT NULL,
       project_id bigint NOT NULL,
       reg_id varchar(255) NOT NULL,
       PRIMARY KEY (auth_id)
);

CREATE INDEX on google_push_credentials(reg_id);
