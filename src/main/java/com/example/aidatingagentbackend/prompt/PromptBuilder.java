package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.dto.AgentInitiative;
import com.example.aidatingagentbackend.dto.ConversationTopicPlan;
import com.example.aidatingagentbackend.dto.PreferenceQuestionPlan;
import com.example.aidatingagentbackend.entity.AgentGoal;
import com.example.aidatingagentbackend.entity.AgentLifeEvent;
import com.example.aidatingagentbackend.entity.AgentProfile;
import com.example.aidatingagentbackend.entity.AgentSelfState;
import com.example.aidatingagentbackend.entity.AgentWorldState;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.entity.CharacterExample;
import com.example.aidatingagentbackend.entity.CharacterPreference;
import com.example.aidatingagentbackend.entity.ChatMessage;
import com.example.aidatingagentbackend.entity.ConversationEvent;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.entity.Relationship;
import com.example.aidatingagentbackend.entity.RelationshipTemperature;
import com.example.aidatingagentbackend.entity.ResponseQualityEvaluation;
import com.example.aidatingagentbackend.entity.State;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptBuilder {

    public Builder builder() {
        return new Builder();
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

        private Character character;
        private State state;
        private Relationship relationship;
        private AgentSelfState agentSelfState;
        private AgentProfile agentProfile;
        private AgentWorldState agentWorldState;
        private AgentGoal agentGoal;
        private AgentInitiative agentInitiative;
        private RelationshipTemperature relationshipTemperature = RelationshipTemperature.NEUTRAL;
        private final List<AgentLifeEvent> agentLifeEvents = new ArrayList<>();
        private final List<ConversationEvent> conversationEvents = new ArrayList<>();
        private PreferenceQuestionPlan preferenceQuestionPlan;
        private ConversationTopicPlan conversationTopicPlan;
        private final List<CharacterPreference> characterPreferences = new ArrayList<>();
        private final List<CharacterExample> characterExamples = new ArrayList<>();
        private final List<Memory> memories = new ArrayList<>();
        private final List<ChatMessage> chatHistory = new ArrayList<>();
        private String userMessage;
        private boolean compactMode;

        public Builder character(Character character) {
            this.character = character;
            return this;
        }

        public Builder state(State state) {
            this.state = state;
            return this;
        }

        public Builder relationship(Relationship relationship) {
            this.relationship = relationship;
            return this;
        }

        public Builder agentSelfState(AgentSelfState agentSelfState) {
            this.agentSelfState = agentSelfState;
            return this;
        }

        public Builder agentProfile(AgentProfile agentProfile) {
            this.agentProfile = agentProfile;
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

        public Builder relationshipTemperature(RelationshipTemperature relationshipTemperature) {
            this.relationshipTemperature = relationshipTemperature == null
                    ? RelationshipTemperature.NEUTRAL
                    : relationshipTemperature;
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

        public Builder chatHistory(List<ChatMessage> history) {
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
            appendState(prompt);
            appendTopic(prompt);
            appendPreference(prompt);
            appendInitiative(prompt);
            appendLifeIfRelevant(prompt);
            appendSharedEvents(prompt);
            appendStyle(prompt);
            appendExamples(prompt);
            appendMemory(prompt);
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

        private void appendState(StringBuilder prompt) {
            prompt.append("[Current State]\n");
            if (state != null) {
                appendInline(prompt, "Emotion", state.getEmotion());
                appendInline(prompt, "Intensity", state.getEmotionIntensity());
            }
            if (agentSelfState != null) {
                appendInline(prompt, "SelfEmotion", agentSelfState.getLastEmotion());
                appendInline(prompt, "Hurt", agentSelfState.getHurt());
                appendInline(prompt, "Anger", agentSelfState.getAnger());
                appendInline(prompt, "Trust", agentSelfState.getTrust());
                appendInline(prompt, "Distance", agentSelfState.getEmotionalDistance());
            }
            if (relationship != null) {
                appendInline(prompt, "Closeness", relationship.getCloseness());
                appendInline(prompt, "Conflict", relationship.getConflictLevel());
                appendInline(prompt, "BreakupRisk", relationship.getBreakupRisk());
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
                appendInline(prompt, "LifeType", agentProfile == null ? null : agentProfile.getLifeType());
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

        private void appendStyle(StringBuilder prompt) {
            prompt.append("[Style]\n");
            appendInline(prompt, "Temperature", relationshipTemperature);
            switch (relationshipTemperature == null ? RelationshipTemperature.NEUTRAL : relationshipTemperature) {
                case FRIENDLY -> prompt.append("Warm, affectionate, soft chat. ㅎㅎ/ㅋㅋ/ㅠㅠ and cute typos are okay. Avoid stiff prose and repeated periods.\n\n");
                case SPICY -> prompt.append("Short banmal, confident, teasing. Use ㅋㅋ/ㅇㅇ/ㄴㄴ/머함 naturally. Almost no periods. Light intimate swearing is okay, but no abuse or coercion.\n\n");
                case CONFLICT_REPAIR -> prompt.append("Calm and guarded. Name feeling, answer directly, leave room for repair. Do not over-question or overuse periods.\n\n");
                case NEUTRAL -> prompt.append("Natural Korean chat, balanced warmth, minimal punctuation, not assistant-like.\n\n");
            }
        }

        private void appendExamples(StringBuilder prompt) {
            if (characterExamples.isEmpty()) {
                return;
            }
            prompt.append("[Style Examples]\n");
            characterExamples.stream()
                    .limit(2)
                    .forEach(example -> {
                        prompt.append("U: ").append(firstText(example.getUserExample(), 80)).append("\n");
                        prompt.append("A: ").append(firstText(example.getAssistantExample(), 120)).append("\n");
                    });
            prompt.append("Copy style only, not content.\n\n");
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
    }
}
