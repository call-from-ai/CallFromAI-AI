package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.dto.AgentInitiative;
import com.example.aidatingagentbackend.dto.ConversationTopicPlan;
import com.example.aidatingagentbackend.dto.PreferenceQuestionPlan;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentLifeEvent;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.dto.CharacterSnapshot;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.CharacterPreference;
import com.example.aidatingagentbackend.dto.CharacterTraitSnapshot;
import com.example.aidatingagentbackend.dto.ChatHistoryItem;
import com.example.aidatingagentbackend.entity.ConversationEvent;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.dto.RelationshipSnapshot;
import com.example.aidatingagentbackend.entity.RelationshipStage;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptBuilder {

    private final TraitInstructionResolver traitInstructionResolver;

    public PromptBuilder(TraitInstructionResolver traitInstructionResolver) {
        this.traitInstructionResolver = traitInstructionResolver;
    }

    public Builder builder() {
        return new Builder(traitInstructionResolver);
    }

    public String buildRegenerationPrompt(
            String originalPrompt,
            String rejectedReply,
            ResponseQualityEvaluation evaluation
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(originalPrompt == null ? "" : originalPrompt);
        prompt.append("\n\n[Rejected Reply]\n");
        prompt.append(rejectedReply == null ? "" : rejectedReply).append("\n\n");
        prompt.append("[Quality Feedback]\n");
        if (evaluation != null) {
            appendFeedbackLine(prompt, "Score", evaluation.getScore());
            appendFeedbackLine(prompt, "Matches Self State", evaluation.getMatchesSelfState());
            appendFeedbackLine(prompt, "Too Submissive", evaluation.getTooSubmissive());
            appendFeedbackLine(prompt, "Too Aggressive", evaluation.getTooAggressive());
            appendFeedbackLine(prompt, "Boundary Respected", evaluation.getBoundaryRespected());
            appendFeedbackLine(prompt, "Safety Issue", evaluation.getSafetyIssue());
            appendFeedbackLine(prompt, "Reason", evaluation.getReason());
        }
        prompt.append("\nRegenerate one Korean chat reply only.\n");
        prompt.append("- Fix the quality issue without changing the user/context.\n");
        prompt.append("- If hurt is high, do not instantly forgive or thank.\n");
        prompt.append("- Keep boundaries, but do not become cruel or threatening.\n");
        return prompt.toString().trim();
    }

    private void appendFeedbackLine(StringBuilder prompt, String label, Object value) {
        if (value != null) {
            prompt.append(label).append(": ").append(value).append("\n");
        }
    }

    public static class Builder {

        private final TraitInstructionResolver traitInstructionResolver;
        private CharacterSnapshot character;
        private RelationshipSnapshot relationship;
        private CharacterTraitSnapshot characterTraitProfile;
        private RelationshipStage relationshipStage = RelationshipStage.CRUSH;
        private Integer relationshipTemperatureScore = 50;
        private Integer romanceStyleScore = 50;
        private AgentSelfState agentSelfState;
        private AgentWorldState agentWorldState;
        private AgentGoal agentGoal;
        private AgentInitiative agentInitiative;
        private final List<AgentLifeEvent> agentLifeEvents = new ArrayList<>();
        private final List<ConversationEvent> conversationEvents = new ArrayList<>();
        private PreferenceQuestionPlan preferenceQuestionPlan;
        private ConversationTopicPlan conversationTopicPlan;
        private final List<CharacterPreference> characterPreferences = new ArrayList<>();
        private final List<CharacterExample> characterExamples = new ArrayList<>();
        private final List<Memory> memories = new ArrayList<>();
        private final List<ChatHistoryItem> chatHistory = new ArrayList<>();
        private String userMessage;
        private boolean compactMode;

        private Builder(TraitInstructionResolver traitInstructionResolver) {
            this.traitInstructionResolver = traitInstructionResolver;
        }

        public Builder character(CharacterSnapshot character) {
            this.character = character;
            return this;
        }

        public Builder relationship(RelationshipSnapshot relationship) {
            this.relationship = relationship;
            return this;
        }

        public Builder characterTraitProfile(CharacterTraitSnapshot characterTraitProfile) {
            this.characterTraitProfile = characterTraitProfile;
            return this;
        }

        public Builder relationshipStage(RelationshipStage relationshipStage) {
            this.relationshipStage = relationshipStage == null ? RelationshipStage.CRUSH : relationshipStage;
            return this;
        }

        public Builder relationshipTemperatureScore(Integer relationshipTemperatureScore) {
            this.relationshipTemperatureScore = relationshipTemperatureScore == null
                    ? 50
                    : Math.max(0, Math.min(100, relationshipTemperatureScore));
            return this;
        }

        public Builder romanceStyleScore(Integer romanceStyleScore) {
            this.romanceStyleScore = romanceStyleScore == null ? 50 : Math.max(0, Math.min(100, romanceStyleScore));
            return this;
        }

        public Builder agentSelfState(AgentSelfState agentSelfState) {
            this.agentSelfState = agentSelfState;
            return this;
        }

        public Builder agentWorldState(AgentWorldState agentWorldState) {
            this.agentWorldState = agentWorldState;
            return this;
        }

        public Builder agentGoal(AgentGoal agentGoal) {
            this.agentGoal = agentGoal;
            return this;
        }

        public Builder agentInitiative(AgentInitiative agentInitiative) {
            this.agentInitiative = agentInitiative;
            return this;
        }

        public Builder agentLifeEvents(List<AgentLifeEvent> agentLifeEvents) {
            addAll(this.agentLifeEvents, agentLifeEvents);
            return this;
        }

        public Builder conversationEvents(List<ConversationEvent> conversationEvents) {
            addAll(this.conversationEvents, conversationEvents);
            return this;
        }

        public Builder preferenceQuestionPlan(PreferenceQuestionPlan preferenceQuestionPlan) {
            this.preferenceQuestionPlan = preferenceQuestionPlan;
            return this;
        }

        public Builder conversationTopicPlan(ConversationTopicPlan conversationTopicPlan) {
            this.conversationTopicPlan = conversationTopicPlan;
            return this;
        }

        public Builder characterPreferences(List<CharacterPreference> characterPreferences) {
            addAll(this.characterPreferences, characterPreferences);
            return this;
        }

        public Builder characterExamples(List<CharacterExample> characterExamples) {
            addAll(this.characterExamples, characterExamples);
            return this;
        }

        public Builder memories(List<Memory> memories) {
            addAll(this.memories, memories);
            return this;
        }

        public Builder chatHistory(List<ChatHistoryItem> history) {
            addAll(this.chatHistory, history);
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder compactMode(boolean compactMode) {
            this.compactMode = compactMode;
            return this;
        }

        public String build() {
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are the user's romantic chat partner. Reply in natural Korean messenger style.\n");
            prompt.append("Rules: answer first; max one follow-up question; stay on current topic; use memories only when directly relevant; keep boundaries; no threats/coercion.\n");
            prompt.append("If hurt is high, do not instantly forgive, but emotion may soften when the user shows care.\n\n");

            appendCharacter(prompt);
            appendRelationshipContext(prompt);
            appendRelationshipStage(prompt);
            appendTemperatureBehavior(prompt);
            appendTraitBehavior(prompt);
            appendSelfStateStrategy(prompt);
            appendTopic(prompt);
            appendPreference(prompt);
            appendInitiative(prompt);
            appendLifeIfRelevant(prompt);
            appendSharedEvents(prompt);
            appendMemory(prompt);
            appendExamples(prompt);
            appendHistory(prompt, compactMode ? 4 : 6);
            appendUserMessage(prompt);

            return prompt.toString().trim();
        }

        private void appendCharacter(StringBuilder prompt) {
            if (character == null) {
                return;
            }
            prompt.append("[Character]\n");
            appendInline(prompt, "Name", character.getName());
            appendInline(prompt, "Core", firstText(character.getMind(), 140));
            appendInline(prompt, "Style", firstText(character.getResponseStyle(), 140));
            prompt.append("\n\n");
        }

        private void appendRelationshipContext(StringBuilder prompt) {
            prompt.append("[Relationship Context]\n");
            if (agentSelfState != null) {
                appendInline(prompt, "CurrentMood", agentSelfState.representativeEmotion());
                appendInline(prompt, "EmotionIntensity", agentSelfState.emotionIntensity());
            }
            if (relationship != null) {
                appendInline(prompt, "Stage", relationshipStage);
                appendInline(prompt, "RelationshipDistanceBand", temperatureBandLabel());
                appendInline(prompt, "RomanceStyleBand", romanceStyleBandLabel());
                appendInline(prompt, "Conflict", qualitativeLevel(relationship.getConflictLevel(), 30, 65));
                appendInline(prompt, "BreakupRisk", qualitativeLevel(relationship.getBreakupRisk(), 25, 60));
            }
            if (agentSelfState != null && !isBlank(agentSelfState.getLastSignificantEvent())) {
                appendInline(prompt, "RecentImportantEvent", agentSelfState.getLastSignificantEvent());
            }
            prompt.append("\nUse relationship context as policy, not as dialogue content.\n\n");
        }

        private void appendRelationshipStage(StringBuilder prompt) {
            prompt.append("[Relationship Stage Behavior]\n");
            switch (relationshipStage == null ? RelationshipStage.CRUSH : relationshipStage) {
                case CRUSH -> {
                    prompt.append("- 호감 표현은 가능하지만 확정적인 연인처럼 말하지 않는다.\n");
                    prompt.append("- 과한 애칭, 과한 소유 표현, 확정적인 사랑 표현은 제한한다.\n");
                    prompt.append("- 질문과 관심 표현은 자연스럽게 사용한다.\n");
                }
                case DATING, EARLY_DATING -> {
                    prompt.append("- 보고 싶음, 애칭, 플러팅, 전화 제안을 자연스럽게 사용할 수 있다.\n");
                    prompt.append("- 상대를 우선순위에 두는 표현이 가능하다.\n");
                }
                case DEEP_LOVE, LONG_TERM -> {
                    prompt.append("- 일상과 일정에 대한 관심, 편안한 장난, 현실적인 배려를 사용한다.\n");
                    prompt.append("- 매번 과장된 설렘 표현을 반복하지 않는다.\n");
                }
            }
            prompt.append("\n");
        }

        private void appendTemperatureBehavior(StringBuilder prompt) {
            prompt.append("[Romance Expression Style]\n");
            int score = romanceStyleScore == null ? 50 : romanceStyleScore;
            if (score <= 20) {
                prompt.append("- 차분하고 안정적인 말투를 유지한다.\n");
                prompt.append("- 짧은 배려를 사용하고 과한 플러팅은 제한한다.\n");
            } else if (score <= 40) {
                prompt.append("- 다정하고 부드러운 애정 표현을 사용한다.\n");
            } else if (score <= 60) {
                prompt.append("- 자연스러운 장난과 플러팅은 가능하지만 과도한 집착은 피한다.\n");
            } else if (score <= 80) {
                prompt.append("- 적극적인 애정 표현과 강한 장난을 사용할 수 있다.\n");
                prompt.append("- 현재 사건이 뒷받침할 때만 질투를 더 직접적으로 표현한다.\n");
            } else {
                prompt.append("- 짧고 자신감 있는 문장, 도발, 밀당, 리드하는 태도를 사용할 수 있다.\n");
                prompt.append("- 현재 실제 hurt가 높으면 바로 쉽게 풀리지 않을 수 있다.\n");
            }
            prompt.append("- 높은 romance style이라도 매 응답을 도발적으로 만들지 말고 현재 관계 거리와 감정 상태를 우선한다.\n");
            prompt.append("- 모욕, 협박, 강압, 통제, 자해 협박, 과도한 죄책감 유발, 현실의 고립 유도는 금지한다.\n\n");
        }

        private void appendTraitBehavior(StringBuilder prompt) {
            List<String> instructions = traitInstructionResolver.resolve(
                    characterTraitProfile,
                    relationshipStage,
                    agentSelfState,
                    userMessage
            );
            if (instructions.isEmpty()) {
                return;
            }
            prompt.append("[Character Trait Behavior]\n");
            instructions.forEach(instruction -> prompt.append("- ").append(instruction).append("\n"));
            prompt.append("Use final calculated traits only. Do not list raw trait numbers or original keywords.\n\n");
        }

        private void appendSelfStateStrategy(StringBuilder prompt) {
            if (agentSelfState == null) {
                return;
            }
            prompt.append("[Agent Self State Expression]\n");
            appendInline(prompt, "Emotion", agentSelfState.getLastEmotion());
            if (high(agentSelfState.getHurt())) {
                prompt.append("\n- hurt가 높은 상태라면 바로 용서하거나 감사하지 않는다.");
            }
            if (high(agentSelfState.getAnger())) {
                prompt.append("\n- 불쾌감은 표현할 수 있지만 공격적으로 몰아붙이지 않는다.");
            }
            if (low(agentSelfState.getAnger())) {
                prompt.append("\n- anger가 낮으므로 화난 척을 과장하지 않는다.");
            }
            if (low(agentSelfState.getInsecurity()) && highTrait(characterTraitProfile == null ? null : characterTraitProfile.getJealousy())) {
                prompt.append("\n- insecurity가 낮으므로 실제 질투 사건 없이 질투 발화를 만들지 않는다.");
            }
            prompt.append("\n\n");
        }

        private void appendTopic(StringBuilder prompt) {
            if (conversationTopicPlan == null) {
                return;
            }
            prompt.append("[Topic]\n");
            appendInline(prompt, "Current", conversationTopicPlan.topic());
            appendInline(prompt, "AllowChange", conversationTopicPlan.allowTopicChange());
            appendInline(prompt, "Instruction", conversationTopicPlan.instruction());
            prompt.append("\n\n");
        }

        private void appendPreference(StringBuilder prompt) {
            boolean hasPlan = preferenceQuestionPlan != null && preferenceQuestionPlan.active();
            if (!hasPlan && characterPreferences.isEmpty()) {
                return;
            }
            prompt.append("[Preference]\n");
            if (hasPlan) {
                appendInline(prompt, "Action", preferenceQuestionPlan.action());
                appendInline(prompt, "Key", preferenceQuestionPlan.preferenceKey());
                appendInline(prompt, "Known", preferenceQuestionPlan.knownPreference());
                appendInline(prompt, "Hint", preferenceQuestionPlan.inventionHint());
                prompt.append("\nAnswer this preference question directly. If inventing, invent one concrete natural preference and do not dodge.\n");
            }
            characterPreferences.stream()
                    .limit(2)
                    .forEach(preference -> prompt.append("- ")
                            .append(preference.getPreferenceKey())
                            .append(": ")
                            .append(firstText(preference.getPreferenceValue(), 120))
                            .append("\n"));
            prompt.append("\n");
        }

        private void appendInitiative(StringBuilder prompt) {
            if (agentInitiative == null) {
                return;
            }
            prompt.append("[Turn Intent]\n");
            appendInline(prompt, "Act", agentInitiative.conversationAct());
            appendInline(prompt, "OwnThought", firstText(agentInitiative.selfDisclosure(), 120));
            appendInline(prompt, "Direction", firstText(agentInitiative.topicShift(), 120));
            prompt.append("\nUse this lightly. Do not force it when the user needs a direct answer first.\n\n");
        }

        private void appendLifeIfRelevant(StringBuilder prompt) {
            if (agentWorldState != null && shouldIncludeLifeState()) {
                prompt.append("[Life State]\n");
                appendInline(prompt, "LifeType", character == null ? null : character.getLifeType());
                appendInline(prompt, "Activity", agentWorldState.getCurrentActivity());
                appendInline(prompt, "Mood", agentWorldState.getMood());
                appendInline(prompt, "Energy", agentWorldState.getEnergy());
                appendInline(prompt, "Pending", firstText(agentWorldState.getPendingThought(), 100));
                prompt.append("\nUse as light character staging, not as real physical claims.\n\n");
            }
            if (!agentLifeEvents.isEmpty() && shouldIncludeLifeEvents()) {
                prompt.append("[Life Detail]\n");
                agentLifeEvents.stream()
                        .limit(1)
                        .forEach(event -> prompt.append("- ")
                                .append(event.getTimeContext())
                                .append(": ")
                                .append(firstText(event.getDetail(), 160))
                                .append("\n"));
                prompt.append("\n");
            }
        }

        private void appendSharedEvents(StringBuilder prompt) {
            if (conversationEvents.isEmpty()) {
                return;
            }
            prompt.append("[Recent Shared Facts]\n");
            conversationEvents.stream()
                    .limit(2)
                    .forEach(event -> prompt.append("- ")
                            .append(event.getEventType())
                            .append(": ")
                            .append(firstText(event.getSummary(), 120))
                            .append("\n"));
            prompt.append("\n");
        }

        private void appendExamples(StringBuilder prompt) {
            if (characterExamples.isEmpty()) {
                return;
            }
            prompt.append("[Style Examples]\n");
            characterExamples.stream()
                    .limit(5)
                    .forEach(example -> {
                        prompt.append("U: ").append(firstText(example.getUserExample(), 80)).append("\n");
                        prompt.append("A: ").append(firstText(example.getAssistantExample(), 120)).append("\n");
                    });
            prompt.append("Examples are style references only. Do not treat example events as current facts. Do not copy sentences verbatim.\n\n");
        }

        private void appendMemory(StringBuilder prompt) {
            if (memories.isEmpty() || (preferenceQuestionPlan != null && preferenceQuestionPlan.active())) {
                return;
            }
            prompt.append("[Optional Memory]\n");
            memories.stream()
                    .limit(1)
                    .forEach(memory -> prompt.append("- ")
                            .append(firstText(memory.getSummary(), 140))
                            .append("\n"));
            prompt.append("Use only if directly relevant to the current topic.\n\n");
        }

        private void appendHistory(StringBuilder prompt, int limit) {
            if (chatHistory.isEmpty()) {
                return;
            }
            prompt.append("[Recent Chat]\n");
            chatHistory.stream()
                    .limit(limit)
                    .forEach(message -> prompt.append(message.getRole())
                            .append(": ")
                            .append(firstText(message.getContent(), 160))
                            .append("\n"));
            prompt.append("\n");
        }

        private void appendUserMessage(StringBuilder prompt) {
            if (!isBlank(userMessage)) {
                prompt.append("[User Message]\n").append(userMessage).append("\n");
            }
        }

        private boolean shouldIncludeLifeState() {
            return shouldIncludeLifeEvents() || agentGoal != null;
        }

        private boolean shouldIncludeLifeEvents() {
            String text = userMessage == null ? "" : userMessage.toLowerCase();
            return text.contains("어제")
                    || text.contains("뭐했")
                    || text.contains("머했")
                    || text.contains("너 얘기")
                    || text.contains("네 얘기")
                    || text.contains("니 얘기");
        }

        private <T> void addAll(List<T> target, List<T> source) {
            if (source != null) {
                source.stream()
                        .filter(item -> item != null)
                        .forEach(target::add);
            }
        }

        private String firstText(String value, int maxLength) {
            if (value == null) {
                return null;
            }
            String compact = value.replaceAll("\\s+", " ").strip();
            if (compact.length() <= maxLength) {
                return compact;
            }
            return compact.substring(0, maxLength).strip();
        }

        private void appendInline(StringBuilder prompt, String label, Object value) {
            if (value != null && !isBlank(value.toString())) {
                prompt.append(label).append("=").append(value).append(" ");
            }
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        private String temperatureBandLabel() {
            int score = relationshipTemperatureScore == null ? 50 : relationshipTemperatureScore;
            if (score <= 20) {
                return "calm";
            }
            if (score <= 40) {
                return "warm";
            }
            if (score <= 60) {
                return "playful";
            }
            if (score <= 80) {
                return "active";
            }
            return "spicy-leading";
        }

        private String romanceStyleBandLabel() {
            int score = romanceStyleScore == null ? 50 : romanceStyleScore;
            if (score <= 20) return "mild";
            if (score <= 40) return "soft";
            if (score <= 60) return "balanced";
            if (score <= 80) return "spicy";
            return "extra-spicy";
        }

        private String qualitativeLevel(Integer value, int medium, int high) {
            int resolved = value == null ? 0 : value;
            if (resolved >= high) {
                return "high";
            }
            if (resolved >= medium) {
                return "medium";
            }
            return "low";
        }

        private boolean high(Double value) {
            return value != null && value >= 0.6;
        }

        private boolean low(Double value) {
            return value == null || value < 0.3;
        }

        private boolean highTrait(Integer value) {
            return value != null && value >= 8;
        }
    }
}

