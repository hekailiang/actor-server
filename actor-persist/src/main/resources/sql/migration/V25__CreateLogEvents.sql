CREATE TABLE log_events (
    id serial not null,
    auth_id bigint not null,
    phone_number bigint not null,
    email varchar(255) NOT NULL,
    klass smallint NOT NULL,
    json_body text NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);

CREATE INDEX ON log_events (auth_id);
CREATE INDEX ON log_events (phone_number);
CREATE INDEX ON log_events (lower(email));
