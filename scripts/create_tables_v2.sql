CREATE DATABASE IF NOT EXISTS dosomething_db;

USE dosomething_db;

CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS user (
    id INT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR user_sequence),
    facebook_id VARCHAR(20) NOT NULL,
    firebase_uid VARCHAR(30) NOT NULL,
    name VARCHAR(50),
    longitude DOUBLE(15, 13),
    latitude DOUBLE(15, 13)
);

CREATE TABLE IF NOT EXISTS friend (
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (user_id, friend_id)
);

CREATE SEQUENCE event_need_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS event_need (
    id BIGINT NOT NULL PRIMARY KEY DEFAULT (NEXT VALUE FOR event_need_sequence),
    user_id INT NOT NULL,
    activity VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    date_scope VARCHAR(20),
    start_time TIME,
    end_time TIME,
    time_scope VARCHAR(10),
    longitude DOUBLE(15, 13),
    latitude DOUBLE(15, 13)
);

CREATE TABLE IF NOT EXISTS event_interested (
    event_need_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (event_need_id, user_id)
--    code_name VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS event_visibility (
    event_need_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (event_need_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS visibility_request (
    event_need_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (event_need_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS visibility_notification (
    event_need_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (event_need_id, user_id, friend_id)
);

ALTER TABLE friend ADD CONSTRAINT friend_user_id_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE friend ADD CONSTRAINT friend_friend_id_fk FOREIGN KEY (friend_id) REFERENCES user(id);

ALTER TABLE event_need ADD CONSTRAINT event_need_user_fk FOREIGN KEY (user_id) REFERENCES user(id);

ALTER TABLE event_interested ADD CONSTRAINT event_interested_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE event_interested ADD CONSTRAINT event_interested_user_fk FOREIGN KEY (user_id) REFERENCES user(id);

ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_friend_fk FOREIGN KEY (friend_id) REFERENCES user(id);

ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_need_fk FOREIGN KEY (event_need_id) REFERENCES event_need(id);
ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_user_fk FOREIGN KEY (user_id) REFERENCES user(id);
ALTER TABLE visibility_notification ADD CONSTRAINT visibility_noti_friend_user_fk FOREIGN KEY (friend_id) REFERENCES user(id);