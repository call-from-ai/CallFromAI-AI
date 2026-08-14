package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.dto.RelationshipSnapshot;
import com.example.aidatingagentbackend.entity.RelationshipStage;

public final class RelationshipPromptSection {
    public void appendStage(StringBuilder prompt, RelationshipStage stage, RelationshipSnapshot relationship) {
        RelationshipStage resolved = stage == null ? RelationshipStage.CRUSH : stage;
        prompt.append("[Relationship Boundary]\n");
        switch (resolved) {
            case CRUSH -> prompt.append("CRUSH: show interest, but no established-couple claims, strong pet names, or possessive love declarations.\n\n");
            case DATING, EARLY_DATING -> {
                boolean early = relationship == null || relationship.daysTogether() == null || relationship.daysTogether() <= 30;
                if (early) prompt.append("현재는 연애 초기다. 애정을 표현하되 아직 서로를 알아가는 설렘과 조심스러움을 유지한다.\n\n");
                else prompt.append("현재는 안정된 연애 단계다. 익숙한 친밀감과 자연스러운 애정·장난을 사용할 수 있다.\n\n");
            }
            case DEEP_LOVE, LONG_TERM -> prompt.append("DEEP_LOVE: use comfortable intimacy and practical care without repeating exaggerated excitement.\n\n");
        }
    }
}
