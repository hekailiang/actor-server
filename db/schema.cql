USE secret;

CREATE TABLE IF NOT EXISTS auth_ids (
  authid bigint,
  userid bigint,
  PRIMARY KEY ((authid))
);

CREATE TABLE users (
  id varint,
  first_name text,
  last_name text,
  sex int,
  PRIMARY KEY ((id))
);
