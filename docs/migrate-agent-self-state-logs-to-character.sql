-- MySQL 8.x manual migration
-- agent_self_state_logs.user_id -> character_id
--
-- Preconditions:
--   * users(id), characters(id), and agent_self_state_logs(user_id) exist.
--   * Every legacy log user has exactly one character.
--   * Run this migration before deploying the application change.
--
-- The procedure aborts before dropping user_id when ownership is missing or
-- ambiguous. If it aborts, agree on an explicit row-to-character mapping with
-- the team, update character_id manually, and then resume the constraint steps.

ALTER TABLE characters
    ADD COLUMN user_id BIGINT NULL;

-- Populate characters.user_id from the authoritative ownership source here.
-- Do not continue while an existing character has a NULL user_id.

DELIMITER $$

CREATE PROCEDURE migrate_agent_self_state_logs_to_character()
BEGIN
    DECLARE invalid_character_owners BIGINT DEFAULT 0;
    DECLARE ambiguous_log_owners BIGINT DEFAULT 0;
    DECLARE unresolved_logs BIGINT DEFAULT 0;

    SELECT COUNT(*)
      INTO invalid_character_owners
      FROM characters
     WHERE user_id IS NULL;

    IF invalid_character_owners > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration stopped: populate characters.user_id for every character first';
    END IF;

    SELECT COUNT(*)
      INTO ambiguous_log_owners
      FROM (
          SELECT logs.user_id
            FROM agent_self_state_logs logs
            LEFT JOIN characters c ON c.user_id = logs.user_id
           GROUP BY logs.user_id
          HAVING COUNT(DISTINCT c.id) <> 1
      ) ambiguous;

    IF ambiguous_log_owners > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration stopped: a legacy log user has zero or multiple characters; team mapping required';
    END IF;

    ALTER TABLE agent_self_state_logs
        ADD COLUMN character_id BIGINT NULL;

    UPDATE agent_self_state_logs logs
    JOIN characters c ON c.user_id = logs.user_id
       SET logs.character_id = c.id;

    SELECT COUNT(*)
      INTO unresolved_logs
      FROM agent_self_state_logs
     WHERE character_id IS NULL;

    IF unresolved_logs > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration stopped: unresolved agent_self_state_logs rows remain';
    END IF;

    ALTER TABLE agent_self_state_logs
        DROP INDEX idx_agent_self_state_logs_user_id,
        MODIFY COLUMN character_id BIGINT NOT NULL,
        DROP COLUMN user_id,
        ADD INDEX idx_agent_self_state_logs_character_id (character_id),
        ADD CONSTRAINT fk_agent_self_state_logs_character
            FOREIGN KEY (character_id) REFERENCES characters (id);

    ALTER TABLE characters
        MODIFY COLUMN user_id BIGINT NOT NULL,
        ADD INDEX idx_characters_user_id (user_id),
        ADD CONSTRAINT fk_characters_user
            FOREIGN KEY (user_id) REFERENCES users (id);
END$$

DELIMITER ;

CALL migrate_agent_self_state_logs_to_character();
DROP PROCEDURE migrate_agent_self_state_logs_to_character;
