CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    facebook_id VARCHAR DEFAULT NULL,
    firebase_uid VARCHAR UNIQUE NOT NULL,
    name VARCHAR DEFAULT NULL,
    email VARCHAR DEFAULT NULL,
    longitude NUMERIC(15, 13) DEFAULT NULL,
    latitude NUMERIC(15, 13) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS friend (
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS event (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    creator_id INT NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_time TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS confirmed_event (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    creator_id INT NOT NULL,
    description VARCHAR(200) NOT NULL,
    created_time TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS event_interested (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_time TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id)
);

CREATE TABLE IF NOT EXISTS event_visibility (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS app_notification (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    payload TEXT NOT NULL,
    type_id INT NOT NULL,
    created_time TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS interest_notification (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_time TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS visibility_notification (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_time TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS event_invite_notification (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_time TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS event_join_notification (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_time TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (event_id, user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS current_activity (
    user_id BIGINT NOT NULL,
    event_id BIGINT,
    updated_time TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id)
);

ALTER TABLE friend ADD CONSTRAINT friend_user_id_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);
ALTER TABLE friend ADD CONSTRAINT friend_friend_id_fk FOREIGN KEY (friend_id) REFERENCES user_profile(id);

ALTER TABLE event ADD CONSTRAINT event_user_fk FOREIGN KEY (creator_id) REFERENCES user_profile(id);

ALTER TABLE event_interested ADD CONSTRAINT event_interested_event_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE event_interested ADD CONSTRAINT event_interested_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);

ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_need_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);
ALTER TABLE event_visibility ADD CONSTRAINT event_visibility_friend_fk FOREIGN KEY (friend_id) REFERENCES user_profile(id);

ALTER TABLE confirmed_event ADD CONSTRAINT active_event_creator_fk FOREIGN KEY (creator_id) REFERENCES user_profile(id);

ALTER TABLE current_activity ADD CONSTRAINT current_activity_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);
ALTER TABLE current_activity ADD CONSTRAINT current_activity_event_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);

ALTER TABLE app_notification ADD CONSTRAINT app_notification_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);

ALTER TABLE interest_notification ADD CONSTRAINT interest_notification_need_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE interest_notification ADD CONSTRAINT interest_notification_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);
ALTER TABLE interest_notification ADD CONSTRAINT interest_notification_friend_fk FOREIGN KEY (friend_id) REFERENCES user_profile(id);

ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_need_fk FOREIGN KEY (event_id) REFERENCES event(id);
ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);
ALTER TABLE visibility_notification ADD CONSTRAINT visibility_notification_friend_fk FOREIGN KEY (friend_id) REFERENCES user_profile(id);

ALTER TABLE event_invite_notification ADD CONSTRAINT event_invite_notification_need_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);
ALTER TABLE event_invite_notification ADD CONSTRAINT event_invite_notification_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);
ALTER TABLE event_invite_notification ADD CONSTRAINT event_invite_notification_friend_fk FOREIGN KEY (friend_id) REFERENCES user_profile(id);

ALTER TABLE event_join_notification ADD CONSTRAINT event_join_notification_need_fk FOREIGN KEY (event_id) REFERENCES confirmed_event(id);
ALTER TABLE event_join_notification ADD CONSTRAINT event_join_notification_user_fk FOREIGN KEY (user_id) REFERENCES user_profile(id);
ALTER TABLE event_join_notification ADD CONSTRAINT event_join_notification_friend_fk FOREIGN KEY (friend_id) REFERENCES user_profile(id);