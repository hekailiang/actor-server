CREATE TABLE user_phones (
  user_id int NOT NULL,
  id int NOT NULL,
  access_salt varchar(255) NOT NULL,
  number bigint NOT NULL,
  title varchar(64) NOT NULL,
  PRIMARY KEY (user_id, id)
);
