CREATE TABLE group_users (
       group_id int NOT NULL,
       user_id int NOT NULL,
       inviter_user_id int NOT NULL,
       invited_at timestamp NOT NULL,
       PRIMARY KEY (group_id, user_id)
);

CREATE INDEX on group_users (user_id);
