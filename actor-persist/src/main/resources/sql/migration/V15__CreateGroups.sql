CREATE TABLE groups (
       id int NOT NULL,
       creator_user_id int NOT NULL,
       access_hash bigint NOT NULL,
       title varchar(255) NOT NULL,
       created_at timestamp NOT NULL,
       title_changer_user_id int NOT NULL,
       title_changed_at timestamp NOT NULL,
       title_change_random_id bigint NOT NULL,
       avatar_changer_user_id int NOT NULL,
       avatar_changed_at timestamp NOT NULL,
       avatar_change_random_id bigint NOT NULL,
       PRIMARY KEY (id)
);
