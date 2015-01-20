CREATE TABLE plain_mails (
       id serial not null,
       random_id bigint not null,
       mail_from varchar(255) NOT NULL,
       recipients text NOT NULL,
       message text NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT now(),
       PRIMARY KEY (id)
);
