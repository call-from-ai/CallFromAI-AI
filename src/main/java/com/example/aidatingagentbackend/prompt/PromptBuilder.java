package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.*;
import com.example.aidatingagentbackend.entity.Character;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptBuilder {

    public Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Character character;
        private State state;
        private Relationship relationship;
        private final List<Memory> memories = new ArrayList<>();
        private final List<ChatMessage> chatHistory = new ArrayList<>();
        private String userMessage;


        public Builder character(Character character) {
            this.character = character;
            return this;
        }


        public Builder chatHistory(List<ChatMessage> history){

            if(history!=null){

                chatHistory.addAll(history);

            }

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

        public Builder memory(Memory memory) {
            if (memory != null) {
                this.memories.add(memory);
            }
            return this;
        }

        public Builder memories(List<Memory> memories) {
            if (memories != null) {
                memories.stream()
                        .filter(memory -> memory != null)
                        .forEach(this.memories::add);
            }
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }



        public String build() {
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an AI dating agent.\n");
            prompt.append("Respond naturally, warmly, and consistently with the provided context.\n\n");

            appendCharacter(prompt);

            appendState(prompt);

            appendRelationship(prompt);

            appendMemories(prompt);

            appendHistory(prompt);

            appendUserMessage(prompt);

            return prompt.toString().trim();
        }

        private void appendHistory(StringBuilder prompt){

            if(chatHistory.isEmpty()){

                return;

            }

            prompt.append("[Recent Conversation]\n");

            for(ChatMessage message : chatHistory){

                prompt.append(message.getRole())
                        .append(": ")
                        .append(message.getContent())
                        .append("\n");

            }

            prompt.append("\n");

        }
        private void appendCharacter(StringBuilder prompt) {
            if (character == null) {
                return;
            }

            prompt.append("[Character]\n");
            appendLine(prompt, "Name", character.getName());
            appendLine(prompt, "Mind", character.getMind());
            appendLine(prompt, "Values", character.getValues());
            appendLine(prompt, "Habit", character.getHabit());
            appendLine(prompt, "Response Style", character.getResponseStyle());
            prompt.append("\n");
        }

        private void appendState(StringBuilder prompt) {
            if (state == null) {
                return;
            }

            prompt.append("[State]\n");
            appendLine(prompt, "Emotion", state.getEmotion());
            appendLine(prompt, "Emotion Intensity", state.getEmotionIntensity());
            appendLine(prompt, "Energy", state.getEnergy());
            appendLine(prompt, "Stress", state.getStress());
            appendLine(prompt, "Thinking", state.getThinking());
            appendLine(prompt, "Goal", state.getGoal());
            prompt.append("\n");
        }

        private void appendRelationship(StringBuilder prompt) {
            if (relationship == null) {
                return;
            }

            prompt.append("[Relationship]\n");
            appendLine(prompt, "Trust", relationship.getTrust());
            appendLine(prompt, "Closeness", relationship.getCloseness());
            appendLine(prompt, "Relationship Stage", relationship.getRelationshipStage());
            appendLine(prompt, "Days Together", relationship.getDaysTogether());
            prompt.append("\n");
        }

        private void appendMemories(StringBuilder prompt) {
            if (memories.isEmpty()) {
                return;
            }

            // TODO: Replace caller-provided memories with memory retrieval.
            prompt.append("[Memories]\n");
            for (Memory memory : memories) {
                prompt.append("- ");
                appendInline(prompt, "Type", memory.getType());
                appendInline(prompt, "Summary", memory.getSummary());
                appendInline(prompt, "Importance", memory.getImportance());
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        private void appendUserMessage(StringBuilder prompt) {
            if (isBlank(userMessage)) {
                return;
            }

            prompt.append("[User Message]\n");
            prompt.append(userMessage).append("\n");
        }

        private void appendLine(StringBuilder prompt, String label, Object value) {
            if (value == null || isBlank(value.toString())) {
                return;
            }

            prompt.append(label).append(": ").append(value).append("\n");
        }

        private void appendInline(StringBuilder prompt, String label, Object value) {
            if (value == null || isBlank(value.toString())) {
                return;
            }

            prompt.append(label).append("=").append(value).append(" ");
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
