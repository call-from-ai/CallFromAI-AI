package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.Context;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.CharacterTraitProfile;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProactiveContactPolicyServiceTests {

    private final ProactiveContactPolicyService service = new ProactiveContactPolicyService();

    @Test
    void highJealousyAndHighTemperatureDoNotForceProactiveContact() {
        CharacterTraitProfile traits = traits(10);

        assertThat(service.shouldSend(context(traits, RelationshipStage.EARLY_DATING, 90, goal("EXPRESS_AFFECTION"), null)))
                .isFalse();
    }

    @Test
    void highHurtOnlyAllowsRepairGoal() {
        AgentSelfState selfState = new AgentSelfState();
        selfState.setHurt(0.8);

        assertThat(service.shouldSend(context(traits(5), RelationshipStage.EARLY_DATING, 50, goal("CHECK_IN"), selfState)))
                .isFalse();
        assertThat(service.shouldSend(context(traits(5), RelationshipStage.EARLY_DATING, 50, goal("REPAIR_RELATIONSHIP"), selfState)))
                .isTrue();
    }

    @Test
    void crushAndLowTemperatureBlockHighPressureGoal() {
        assertThat(service.shouldSend(context(traits(5), RelationshipStage.CRUSH, 30, goal("EXPRESS_AFFECTION"), null)))
                .isFalse();
    }

    private Context context(
            CharacterTraitProfile traits,
            RelationshipStage stage,
            Integer temperatureScore,
            AgentGoal goal,
            AgentSelfState selfState
    ) {
        return new Context(
                null,
                null,
                null,
                traits,
                stage,
                temperatureScore,
                selfState,
                null,
                null,
                goal,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private CharacterTraitProfile traits(int jealousy) {
        CharacterTraitProfile traits = new CharacterTraitProfile();
        traits.setJealousy(jealousy);
        return traits;
    }

    private AgentGoal goal(String goalType) {
        AgentGoal goal = new AgentGoal();
        goal.setGoalType(goalType);
        return goal;
    }
}
