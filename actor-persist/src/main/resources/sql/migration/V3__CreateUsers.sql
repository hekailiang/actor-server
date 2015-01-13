CREATE TABLE users (
       id int NOT NULL,
       access_salt text NOT NULL,
       name varchar(255) NOT NULL,
       country_code varchar(2) NOT NULL,
       sex int NOT NULL,
       state int NOT NULL,
       PRIMARY KEY (id)
);
