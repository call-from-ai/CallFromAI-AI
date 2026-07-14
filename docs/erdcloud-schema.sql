-- ERDCloud import schema
-- Database: MySQL 8.x
--
-- Import at: ERDCloud > 새 ERD > 가져오기/Import > SQL
--
-- NOTE:
-- `users` and `characters` are logical reference tables. They do not currently
-- have matching JPA entities in this repository, but are included so ERDCloud
-- can draw relationships for user_id and character_id columns.

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (id)
);

CREATE TABLE characters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_characters_user_id (user_id),
    CONSTRAINT fk_characters_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE agent_self_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NULL,
    character_id BIGINT NOT NULL,
    affection DOUBLE NULL,
    trust DOUBLE NULL,
    hurt DOUBLE NULL,
    anger DOUBLE NULL,
    insecurity DOUBLE NULL,
    disappointment DOUBLE NULL,
    emotional_distance DOUBLE NULL,
    last_emotion VARCHAR(255) NULL,
    last_significant_event VARCHAR(255) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_agent_self_state_character UNIQUE (character_id),
    CONSTRAINT fk_agent_self_states_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE agent_self_state_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NOT NULL,
    previous_hurt DOUBLE NULL,
    next_hurt DOUBLE NULL,
    previous_trust DOUBLE NULL,
    next_trust DOUBLE NULL,
    previous_anger DOUBLE NULL,
    next_anger DOUBLE NULL,
    previous_insecurity DOUBLE NULL,
    next_insecurity DOUBLE NULL,
    event_type VARCHAR(255) NULL,
    severity DOUBLE NULL,
    delta_reason TEXT NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_agent_self_state_logs_character_id (character_id),
    CONSTRAINT fk_agent_self_state_logs_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE agent_world_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    current_activity VARCHAR(255) NULL,
    location VARCHAR(255) NULL,
    time_context VARCHAR(255) NULL,
    mood VARCHAR(255) NULL,
    energy INT NULL,
    stress INT NULL,
    loneliness INT NULL,
    pending_thought TEXT NULL,
    last_updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_agent_world_states_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE agent_goals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    goal_type VARCHAR(255) NULL,
    description TEXT NULL,
    priority INT NULL,
    status VARCHAR(255) NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_agent_goals_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE agent_life_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    event_date DATE NULL,
    time_context VARCHAR(255) NULL,
    summary TEXT NULL,
    detail TEXT NULL,
    emotion VARCHAR(255) NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_agent_life_events_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE character_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    preference_key VARCHAR(255) NULL,
    preference_value TEXT NULL,
    source VARCHAR(255) NULL,
    confidence DOUBLE NULL,
    stability VARCHAR(255) NULL,
    created_from_message TEXT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_character_preferences_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE character_examples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    event_type VARCHAR(255) NULL COMMENT 'AgentEventType enum',
    relationship_temperature VARCHAR(255) NULL COMMENT 'RelationshipTemperature enum',
    relationship_stage VARCHAR(255) NULL,
    min_temperature_score INT NULL,
    max_temperature_score INT NULL,
    romance_style_band VARCHAR(255) NULL COMMENT 'RomanceStyleBand enum',
    user_example TEXT NULL,
    assistant_example TEXT NULL,
    tone_tag VARCHAR(255) NULL,
    priority INT NULL,
    active BIT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_character_examples_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE conversation_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    event_type VARCHAR(255) NULL,
    summary TEXT NULL,
    agent_reaction TEXT NULL,
    importance DOUBLE NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_conversation_events_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE memories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    type VARCHAR(255) NULL COMMENT 'MemoryType: FACT, EPISODE, INSIDE_JOKE',
    summary TEXT NULL,
    embedding TEXT NULL,
    importance INT NULL,
    last_retrieved_at DATETIME(6) NULL,
    retrieval_count INT NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_memories_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE turning_points (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NULL,
    event_type VARCHAR(255) NULL,
    summary TEXT NULL,
    impact_emotion VARCHAR(255) NULL,
    impact_score INT NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_turning_points_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);

CREATE TABLE response_quality_evaluations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    character_id BIGINT NOT NULL,
    matches_self_state BIT NULL,
    too_submissive BIT NULL,
    too_aggressive BIT NULL,
    boundary_respected BIT NULL,
    safety_issue BIT NULL,
    regenerated BIT NULL,
    score DOUBLE NULL,
    reason TEXT NULL,
    user_message TEXT NULL,
    assistant_reply TEXT NULL,
    created_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_response_quality_evaluations_character
        FOREIGN KEY (character_id) REFERENCES characters (id)
);
