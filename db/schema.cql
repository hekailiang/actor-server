CREATE KEYSPACE IF NOT EXISTS secret WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE secret;

CREATE TABLE auth_ids (
  authid bigint,
  userid bigint,
  PRIMARY KEY ((authid))
);

CREATE TABLE auth_sms_codes (
  phone_number bigint,
  sms_code text,
  sms_hash text,
  PRIMARY KEY ((phone_number))
);

CREATE TABLE phones (
  number bigint,
  user_id int,
  PRIMARY KEY ((number))
);

CREATE TABLE sessions (
  authid bigint,
  sessionid bigint,
  PRIMARY KEY ((authid, sessionid))
);

CREATE TABLE users (
  id int,
  access_hash bigint,
  first_name text,
  last_name text,
  sex int,
  PRIMARY KEY ((id))
);
