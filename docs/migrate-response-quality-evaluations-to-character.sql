-- MySQL 8.x manual migration
-- Existing values were populated from AIProcessingService.characterId despite
-- the legacy Java/column name `user_id`, so no BE database lookup is required.

ALTER TABLE response_quality_evaluations
    CHANGE COLUMN user_id character_id BIGINT NOT NULL;

CREATE INDEX idx_response_quality_evaluations_character_id
    ON response_quality_evaluations (character_id);
