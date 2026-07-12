-- Precondition: remove duplicate rows before deployment. This migration intentionally fails
-- when duplicate character/profile/priority/trait data exists so no row is silently discarded.
ALTER TABLE character_trait_profiles
    MODIFY character_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_trait_profile_character UNIQUE (character_id);

ALTER TABLE character_trait_selections
    MODIFY character_trait_profile_id BIGINT NOT NULL,
    MODIFY trait VARCHAR(255) NOT NULL,
    MODIFY priority INT NOT NULL,
    ADD CONSTRAINT uk_trait_selection_keyword UNIQUE (character_trait_profile_id, trait),
    ADD CONSTRAINT uk_trait_selection_priority UNIQUE (character_trait_profile_id, priority);

ALTER TABLE relationships
    MODIFY character_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_relationship_character UNIQUE (character_id);

ALTER TABLE agent_self_states
    MODIFY character_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_agent_self_state_character UNIQUE (character_id);

ALTER TABLE character_examples
    ADD COLUMN romance_style_band VARCHAR(255) NULL;
