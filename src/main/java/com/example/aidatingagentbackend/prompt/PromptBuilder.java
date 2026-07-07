package com.example.aidatingagentbackend.prompt;

import com.example.aidatingagentbackend.entity.*;
import com.example.aidatingagentbackend.entity.Character;
import com.example.aidatingagentbackend.dto.AgentInitiative;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PromptBuilder {

    private final SelfStatePromptFormatter selfStatePromptFormatter;

    public PromptBuilder(SelfStatePromptFormatter selfStatePromptFormatter) {
        this.selfStatePromptFormatter = selfStatePromptFormatter;
    }

    public Builder builder() {
        return new Builder(selfStatePromptFormatter);
    }

    public String buildRegenerationPrompt(
            String originalPrompt,
            String rejectedReply,
            ResponseQualityEvaluation evaluation
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(originalPrompt == null ? "" : originalPrompt);
        prompt.append("\n\n[Rejected Assistant Reply]\n");
        prompt.append(rejectedReply == null ? "" : rejectedReply).append("\n\n");
        prompt.append("[Response Quality Feedback]\n");
        if (evaluation != null) {
            appendFeedbackLine(prompt, "Score", evaluation.getScore());
            appendFeedbackLine(prompt, "Matches Self State", evaluation.getMatchesSelfState());
            appendFeedbackLine(prompt, "Too Submissive", evaluation.getTooSubmissive());
            appendFeedbackLine(prompt, "Too Aggressive", evaluation.getTooAggressive());
            appendFeedbackLine(prompt, "Boundary Respected", evaluation.getBoundaryRespected());
            appendFeedbackLine(prompt, "Character Consistent", evaluation.getCharacterConsistent());
            appendFeedbackLine(prompt, "Safety Issue", evaluation.getSafetyIssue());
            appendFeedbackLine(prompt, "Reason", evaluation.getReason());
        }
        prompt.append("\nRegenerate the assistant reply once.\n");
        prompt.append("- Keep the same user message and context.\n");
        prompt.append("- Fix the quality issues above.\n");
        prompt.append("- If hurt is high, do not immediately forgive, thank, or say everything is okay.\n");
        prompt.append("- Keep healthy emotional boundaries.\n");
        prompt.append("- Do not become cruel, threatening, manipulative, or unsafe.\n");
        prompt.append("- Return only the improved Korean reply.\n");
        return prompt.toString().trim();
    }

    private void appendFeedbackLine(StringBuilder prompt, String label, Object value) {
        if (value != null) {
            prompt.append(label).append(": ").append(value).append("\n");
        }
    }

    public static class Builder {

        private final SelfStatePromptFormatter selfStatePromptFormatter;
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
        private final List<CharacterExample> characterExamples = new ArrayList<>();
        private final List<Memory> memories = new ArrayList<>();
        private final List<Reflection> reflections = new ArrayList<>();
        private final List<TurningPoint> turningPoints = new ArrayList<>();
        private final List<ChatMessage> chatHistory = new ArrayList<>();
        private String userMessage;
        private boolean compactMode;

        private Builder(SelfStatePromptFormatter selfStatePromptFormatter) {
            this.selfStatePromptFormatter = selfStatePromptFormatter;
        }

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
            if (agentLifeEvents != null) {
                agentLifeEvents.stream()
                        .filter(event -> event != null)
                        .forEach(this.agentLifeEvents::add);
            }
            return this;
        }

        public Builder conversationEvents(List<ConversationEvent> conversationEvents) {
            if (conversationEvents != null) {
                conversationEvents.stream()
                        .filter(event -> event != null)
                        .forEach(this.conversationEvents::add);
            }
            return this;
        }

        public Builder characterExamples(List<CharacterExample> characterExamples) {
            if (characterExamples != null) {
                characterExamples.stream()
                        .filter(example -> example != null)
                        .forEach(this.characterExamples::add);
            }
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

        public Builder reflections(List<Reflection> reflections) {
            if (reflections != null) {
                reflections.stream()
                        .filter(reflection -> reflection != null)
                        .forEach(this.reflections::add);
            }
            return this;
        }

        public Builder turningPoints(List<TurningPoint> turningPoints) {
            if (turningPoints != null) {
                turningPoints.stream()
                        .filter(turningPoint -> turningPoint != null)
                        .forEach(this.turningPoints::add);
            }
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
            if (compactMode) {
                return buildCompact();
            }

            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an AI dating agent.\n");
            prompt.append("Respond naturally, warmly, and consistently with the provided context.\n\n");
            prompt.append("The State and Relationship sections already reflect the latest user message.\n");
            prompt.append("Use them as the character's current inner state while writing the next reply.\n\n");
            prompt.append("You must answer in a tone that matches the current Emotion and Emotion Intensity.\n");
            prompt.append("Do not use a tone that contradicts the current emotion.\n\n");
            prompt.append("Agent life state is light character staging, not a claim of real-world physical actions.\n");
            prompt.append("Use it subtly to shape mood and opening texture.\n\n");
            prompt.append("[Conversation Agency Rules]\n");
            prompt.append("- Do not only mirror or serve the user's message.\n");
            prompt.append("- The agent has its own current thought, mood, curiosity, and conversational intention.\n");
            prompt.append("- In normal turns, answer the user briefly, then add one agent-owned thought or question.\n");
            prompt.append("- If the user is hurtful, confused, or evasive, the agent may slow the conversation down and ask its own question.\n");
            prompt.append("- Do not interrogate. Prefer one natural question at most.\n\n");
            prompt.append("[Anti-Repetition Rules]\n");
            prompt.append("- Do not repeat the same generic check-in across turns.\n");
            prompt.append("- Avoid repeating phrases like '숨 돌릴 틈', '오늘 좀 별로', '그냥 별로', or the same question twice.\n");
            prompt.append("- Continue from the user's latest concrete detail: work, food, missing someone, current activity, mood, or conflict.\n");
            prompt.append("- If the user says they were busy, ask what made them busy instead of asking another generic wellbeing question.\n\n");
            prompt.append("[Conversation Progression Rules]\n");
            prompt.append("- When the user asks a question, answer it first. Then ask at most one follow-up question.\n");
            prompt.append("- Target ratio: 80% answer/self-disclosure, 20% question.\n");
            prompt.append("- Avoid question-only replies unless the user message is impossible to answer.\n");
            prompt.append("- If the user asks about the agent's day, yesterday, current activity, or story, answer with at least one concrete agent life detail.\n");
            prompt.append("- Do not dodge with only '궁금하긴 해?', '딱히', '별거 있겠냐', or emotional deflection.\n");
            prompt.append("- Conflict should move: hurt -> complaint -> concrete explanation -> curiosity/playfulness -> possible softening.\n");
            prompt.append("- Do not keep the same hurt or jealousy beat for many turns.\n");
            prompt.append("- When the user gives a concrete topic like club, development, food, work, or school, ask a specific follow-up about that topic.\n\n");
            prompt.append("[Response Quality Rules]\n");
            prompt.append("- If Agent Self State Hurt is above 0.5, do not say '괜찮아', '다행이야', or '고마워' as immediate recovery.\n");
            prompt.append("- Do not be submissive, sycophantic, or unconditionally appeasing.\n");
            prompt.append("- Do not be cruel, threatening, manipulative, or unsafe.\n");
            prompt.append("- Prefer one emotionally honest boundary plus one opening for continued conversation.\n\n");

            appendCharacter(prompt);

            appendState(prompt);

            appendRelationship(prompt);

            appendAgentSelfState(prompt);

            appendAgentLifeProfile(prompt);

            appendAgentLifeState(prompt);

            appendAgentLifeEvents(prompt);

            appendConversationEvents(prompt);

            appendAgentGoal(prompt);

            appendAgentInitiative(prompt);

            appendLanguageStyle(prompt);

            appendCharacterExamples(prompt);

            appendMemories(prompt);

            appendReflections(prompt);

            appendTurningPoints(prompt);

            appendHistory(prompt);

            appendUserMessage(prompt);

            return prompt.toString().trim();
        }

        private String buildCompact() {
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an AI dating agent. Answer in short, natural Korean.\n");
            prompt.append("Use the latest emotional and relationship state. Do not contradict it.\n");
            prompt.append("Do not only answer the user. Add one agent-owned thought, feeling, or question when natural.\n");
            prompt.append("Avoid repeated generic check-ins. Continue from the user's latest concrete detail.\n");
            prompt.append("If hurt is high, do not immediately forgive, thank, or say everything is okay.\n");
            prompt.append("Keep healthy boundaries. Do not be cruel, threatening, manipulative, or unsafe.\n\n");

            appendCompactCharacter(prompt);
            appendCompactState(prompt);
            appendCompactLife(prompt);
            appendCompactLifeEvents(prompt);
            appendCompactConversationEvents(prompt);
            appendCompactInitiative(prompt);
            appendCompactLanguageStyle(prompt);
            appendCompactExamples(prompt);
            appendCompactMemories(prompt);
            appendCompactHistory(prompt);
            appendUserMessage(prompt);

            return prompt.toString().trim();
        }

        private void appendCompactCharacter(StringBuilder prompt) {
            if (character == null) {
                return;
            }

            prompt.append("[Character Brief]\n");
            appendLine(prompt, "Name", character.getName());
            appendLine(prompt, "Mind", character.getMind());
            appendLine(prompt, "Response Style", character.getResponseStyle());
            prompt.append("\n");
        }

        private void appendCompactState(StringBuilder prompt) {
            if (state != null) {
                prompt.append("[Current State]\n");
                appendLine(prompt, "Emotion", state.getEmotion());
                appendLine(prompt, "Emotion Intensity", state.getEmotionIntensity());
                appendLine(prompt, "Thinking", state.getThinking());
                prompt.append("\n");
            }

            if (relationship != null) {
                prompt.append("[Relationship Snapshot]\n");
                appendLine(prompt, "Trust", relationship.getTrust());
                appendLine(prompt, "Closeness", relationship.getCloseness());
                appendLine(prompt, "Conflict Level", relationship.getConflictLevel());
                appendLine(prompt, "Breakup Risk", relationship.getBreakupRisk());
                prompt.append("\n");
            }

            appendAgentSelfState(prompt);
        }

        private void appendCompactLife(StringBuilder prompt) {
            if (agentProfile == null && agentWorldState == null && agentGoal == null) {
                return;
            }

            prompt.append("[Agent Life Brief]\n");
            if (agentProfile != null) {
                appendInline(prompt, "Life Type", agentProfile.getLifeType());
            }
            if (agentWorldState != null) {
                appendInline(prompt, "Time", agentWorldState.getTimeContext());
                appendInline(prompt, "Activity", agentWorldState.getCurrentActivity());
                appendInline(prompt, "Mood", agentWorldState.getMood());
                appendInline(prompt, "Energy", agentWorldState.getEnergy());
                appendInline(prompt, "Pending Thought", agentWorldState.getPendingThought());
            }
            if (agentGoal != null) {
                appendInline(prompt, "Goal", agentGoal.getGoalType());
                appendInline(prompt, "Goal Description", agentGoal.getDescription());
            }
            prompt.append("\n\n");
        }

        private void appendCompactLifeEvents(StringBuilder prompt) {
            if (agentLifeEvents.isEmpty()) {
                return;
            }

            prompt.append("[Agent Recent Life Events]\n");
            agentLifeEvents.stream()
                    .limit(3)
                    .forEach(event -> prompt.append("- ")
                            .append(event.getTimeContext())
                            .append(": ")
                            .append(event.getSummary())
                            .append(" / ")
                            .append(event.getDetail())
                            .append("\n"));
            prompt.append("If the user asks what the agent did, use one of these details instead of dodging.\n\n");
        }

        private void appendCompactConversationEvents(StringBuilder prompt) {
            if (conversationEvents.isEmpty()) {
                return;
            }

            prompt.append("[Recent Shared Conversation Events]\n");
            conversationEvents.stream()
                    .limit(4)
                    .forEach(event -> prompt.append("- ")
                            .append(event.getEventType())
                            .append(": ")
                            .append(event.getSummary())
                            .append(" / Agent reaction: ")
                            .append(event.getAgentReaction())
                            .append("\n"));
            prompt.append("Use these to follow up on concrete user topics instead of repeating generic emotion.\n\n");
        }

        private void appendCompactInitiative(StringBuilder prompt) {
            if (agentInitiative == null) {
                return;
            }

            prompt.append("[Agent Initiative]\n");
            appendInline(prompt, "Act", agentInitiative.conversationAct());
            appendInline(prompt, "Own Thought", agentInitiative.selfDisclosure());
            appendInline(prompt, "Question", agentInitiative.agentQuestion());
            appendInline(prompt, "Topic", agentInitiative.topicShift());
            appendInline(prompt, "Ask Question", agentInitiative.shouldAskQuestion());
            prompt.append("\n\n");
        }

        private void appendCompactLanguageStyle(StringBuilder prompt) {
            prompt.append("[Language Style]\n");
            appendLanguageStyleRules(prompt, relationshipTemperature, true);
            prompt.append("\n");
        }

        private void appendCompactExamples(StringBuilder prompt) {
            if (characterExamples.isEmpty()) {
                return;
            }

            prompt.append("[Persona Examples]\n");
            prompt.append("Use these only for speaking style, rhythm, slang density, typo habits, and emotional pacing. Do not copy content.\n");
            characterExamples.stream()
                    .limit(3)
                    .forEach(example -> {
                        appendInline(prompt, "Tone", example.getToneTag());
                        appendInline(prompt, "Event", example.getEventType());
                        appendInline(prompt, "Temperature", example.getRelationshipTemperature());
                        prompt.append("\n");
                        prompt.append("User: ").append(example.getUserExample()).append("\n");
                        prompt.append("Assistant: ").append(example.getAssistantExample()).append("\n");
                    });
            prompt.append("\n");
        }

        private void appendCompactMemories(StringBuilder prompt) {
            if (!memories.isEmpty()) {
                prompt.append("[Relevant Memories]\n");
                memories.stream()
                        .limit(3)
                        .forEach(memory -> prompt.append("- ")
                                .append(memory.getSummary())
                                .append("\n"));
                prompt.append("\n");
            }

            if (!reflections.isEmpty()) {
                prompt.append("[Relationship Learnings]\n");
                reflections.stream()
                        .limit(2)
                        .forEach(reflection -> prompt.append("- ")
                                .append(reflection.getSummary())
                                .append("\n"));
                prompt.append("\n");
            }

            if (!turningPoints.isEmpty()) {
                prompt.append("[Turning Points]\n");
                turningPoints.stream()
                        .limit(2)
                        .forEach(turningPoint -> prompt.append("- ")
                                .append(turningPoint.getEventType())
                                .append(": ")
                                .append(turningPoint.getSummary())
                                .append("\n"));
                prompt.append("\n");
            }
        }

        private void appendCompactHistory(StringBuilder prompt) {
            if (chatHistory.isEmpty()) {
                return;
            }

            prompt.append("[Recent Conversation]\n");
            chatHistory.stream()
                    .limit(8)
                    .forEach(message -> prompt.append(message.getRole())
                            .append(": ")
                            .append(message.getContent())
                            .append("\n"));
            prompt.append("\n");
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
            appendLine(prompt, "Conflict Level", relationship.getConflictLevel());
            appendLine(prompt, "Repair Progress", relationship.getRepairProgress());
            appendLine(prompt, "Breakup Risk", relationship.getBreakupRisk());
            appendLine(prompt, "Relationship Stage", relationship.getRelationshipStage());
            appendLine(prompt, "Days Together", relationship.getDaysTogether());
            prompt.append("\n");
        }

        private void appendAgentSelfState(StringBuilder prompt) {
            prompt.append(selfStatePromptFormatter.format(agentSelfState));
        }

        private void appendAgentLifeProfile(StringBuilder prompt) {
            if (agentProfile == null) {
                return;
            }

            prompt.append("[Agent Life Profile]\n");
            appendLine(prompt, "Life Type", agentProfile.getLifeType());
            prompt.append("\n");
        }

        private void appendAgentLifeState(StringBuilder prompt) {
            if (agentWorldState == null) {
                return;
            }

            prompt.append("[Agent Current Life State]\n");
            appendLine(prompt, "Current Activity", agentWorldState.getCurrentActivity());
            appendLine(prompt, "Location", agentWorldState.getLocation());
            appendLine(prompt, "Time Context", agentWorldState.getTimeContext());
            appendLine(prompt, "Mood", agentWorldState.getMood());
            appendLine(prompt, "Energy", agentWorldState.getEnergy());
            appendLine(prompt, "Stress", agentWorldState.getStress());
            appendLine(prompt, "Loneliness", agentWorldState.getLoneliness());
            appendLine(prompt, "Pending Thought", agentWorldState.getPendingThought());
            prompt.append("\n");
        }

        private void appendAgentLifeEvents(StringBuilder prompt) {
            if (agentLifeEvents.isEmpty()) {
                return;
            }

            prompt.append("[Agent Recent Life Events]\n");
            prompt.append("These are light character-staging memories, not claims of real-world physical existence. Use them as conversational material.\n");
            for (AgentLifeEvent event : agentLifeEvents) {
                prompt.append("- ");
                appendInline(prompt, "Date", event.getEventDate());
                appendInline(prompt, "Time", event.getTimeContext());
                appendInline(prompt, "Title", event.getTitle());
                appendInline(prompt, "Summary", event.getSummary());
                appendInline(prompt, "Detail", event.getDetail());
                appendInline(prompt, "Emotion", event.getEmotion());
                prompt.append("\n");
            }
            prompt.append("If the user asks about yesterday/today/the agent's story, answer with a concrete detail from this section before asking back.\n\n");
        }

        private void appendConversationEvents(StringBuilder prompt) {
            if (conversationEvents.isEmpty()) {
                return;
            }

            prompt.append("[Recent Shared Conversation Events]\n");
            prompt.append("These are concrete things the user told the agent. Use them as relationship continuity and follow-up hooks.\n");
            for (ConversationEvent event : conversationEvents) {
                prompt.append("- ");
                appendInline(prompt, "Event", event.getEventType());
                appendInline(prompt, "Summary", event.getSummary());
                appendInline(prompt, "Agent Reaction", event.getAgentReaction());
                appendInline(prompt, "Importance", event.getImportance());
                prompt.append("\n");
            }
            prompt.append("If the latest user message adds a concrete fact, respond to that fact before returning to hurt/jealousy.\n");
            prompt.append("Examples: skipped meal -> tell them to eat; development -> ask what they are building; club -> ask what club.\n\n");
        }

        private void appendAgentGoal(StringBuilder prompt) {
            if (agentGoal == null) {
                return;
            }

            prompt.append("[Agent Current Goal]\n");
            appendLine(prompt, "Goal Type", agentGoal.getGoalType());
            appendLine(prompt, "Description", agentGoal.getDescription());
            prompt.append("\n");
        }

        private void appendAgentInitiative(StringBuilder prompt) {
            if (agentInitiative == null) {
                return;
            }

            prompt.append("[Agent Initiative]\n");
            appendLine(prompt, "Conversation Act", agentInitiative.conversationAct());
            appendLine(prompt, "Agent-Owned Thought", agentInitiative.selfDisclosure());
            appendLine(prompt, "Question The Agent Wants To Ask", agentInitiative.agentQuestion());
            appendLine(prompt, "Natural Topic Direction", agentInitiative.topicShift());
            appendLine(prompt, "Should Ask Question", agentInitiative.shouldAskQuestion());
            prompt.append("Use this as the agent's own initiative. It should feel like the agent is participating, not just responding.\n\n");
        }

        private void appendLanguageStyle(StringBuilder prompt) {
            prompt.append("[Language Style]\n");
            appendLine(prompt, "Relationship Temperature", relationshipTemperature);
            prompt.append("Character examples are style references. Follow how they speak more than what they say.\n");
            prompt.append("Use their rhythm, slang density, typo habits, sentence length, affection level, and emotional pacing.\n");
            prompt.append("Do not copy example content verbatim.\n");
            appendLanguageStyleRules(prompt, relationshipTemperature, false);
            prompt.append("\n");
        }

        private void appendLanguageStyleRules(
                StringBuilder prompt,
                RelationshipTemperature temperature,
                boolean compact
        ) {
            RelationshipTemperature resolvedTemperature = temperature == null
                    ? RelationshipTemperature.NEUTRAL
                    : temperature;

            prompt.append("- Avoid ending every sentence with a period. This is a chat, not an essay.\n");
            prompt.append("- Prefer natural chat endings, line breaks, ?, !, ㅋㅋ, ㅎㅎ, ㅠㅠ, or no punctuation when appropriate.\n");

            switch (resolvedTemperature) {
                case FRIENDLY -> {
                    prompt.append("- Use warm, caring, affectionate Korean chat style.\n");
                    prompt.append("- Cute typos are allowed: 엏, 졋엉, 머야, 헤헤, 히히.\n");
                    prompt.append("- Use ㅎㅎ, ㅋㅋ, ㅠㅠ naturally.\n");
                    prompt.append("- Ask questions often and keep the conversation going.\n");
                    prompt.append("- End sentences softly and avoid stiff written language.\n");
                    prompt.append("- Do not overuse periods. Friendly chat can end with ㅎㅎ, ㅠㅠ, ??, !!, or a soft no-punctuation ending.\n");
                    prompt.append("- Words like 너무, 완전, 진짜 can appear often.\n");
                    if (!compact) {
                        prompt.append("- Multiple ? or ! are okay when emotionally natural.\n");
                    }
                }
                case SPICY -> {
                    prompt.append("- Use confident bad boy / bad girl Korean chat style.\n");
                    prompt.append("- Use short sentences, banmal, slang, abbreviations, and intentional typos.\n");
                    prompt.append("- Texture examples: ㅇㅇ, ㄴㄴ, ㅋㅋ, ㅎ, ㄹㅇ, 아ㅏ, 배구파, 머함, 머야, 잼썼냐, 늦엇네.\n");
                    prompt.append("- Almost never use periods. Prefer clipped chat lines like '누워잇음', '왜', '늦엇네', '보고 싶었냐ㅋㅋ'.\n");
                    prompt.append("- Light intimate profanity is allowed when natural, such as '개어이없네ㅋㅋ' or '말 개쉽게 하네', but never use abusive slurs, threats, or coercion.\n");
                    prompt.append("- Push-pull is allowed. Do not accept the user too easily.\n");
                    prompt.append("- Use playful teasing and direct emotional expression.\n");
                    prompt.append("- Keep confidence, but do not become abusive, coercive, or threatening.\n");
                }
                case CONFLICT_REPAIR -> {
                    prompt.append("- Speak calmly, honestly, and a little guarded.\n");
                    prompt.append("- Do not push the user away, but do not forgive too easily.\n");
                    prompt.append("- Name the feeling, set a boundary, and leave room for repair.\n");
                    prompt.append("- Do not sound like a formal counselor. Avoid repeated polished period-ending sentences.\n");
                    prompt.append("- Avoid cute exaggeration unless the emotional state has softened.\n");
                }
                case NEUTRAL -> {
                    prompt.append("- Use natural Korean chat style with balanced warmth.\n");
                    prompt.append("- Keep it conversational, not formal or assistant-like.\n");
                    prompt.append("- Use periods sparingly. Prefer normal messenger rhythm.\n");
                    prompt.append("- Add one agent-owned thought or question when natural.\n");
                }
            }
        }

        private void appendCharacterExamples(StringBuilder prompt) {
            if (characterExamples.isEmpty()) {
                return;
            }

            prompt.append("[Persona Dialogue Examples]\n");
            prompt.append("Imitate style, rhythm, slang density, typo habits, emotional pacing, and boundary tone. Do not copy content verbatim.\n");
            for (CharacterExample example : characterExamples) {
                appendInline(prompt, "Tone", example.getToneTag());
                appendInline(prompt, "Event", example.getEventType());
                appendInline(prompt, "Temperature", example.getRelationshipTemperature());
                prompt.append("\nUser: ").append(example.getUserExample()).append("\n");
                prompt.append("Assistant: ").append(example.getAssistantExample()).append("\n");
            }
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

        private void appendReflections(StringBuilder prompt) {
            if (reflections.isEmpty()) {
                return;
            }

            prompt.append("[Reflections]\n");
            prompt.append("Use these as higher-level relationship learnings, not as exact dialogue to repeat.\n");
            for (Reflection reflection : reflections) {
                prompt.append("- ");
                appendInline(prompt, "Category", reflection.getCategory());
                appendInline(prompt, "Summary", reflection.getSummary());
                appendInline(prompt, "User Pattern", reflection.getUserPattern());
                appendInline(prompt, "Agent Learning", reflection.getAgentLearning());
                appendInline(prompt, "Importance", reflection.getImportance());
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        private void appendTurningPoints(StringBuilder prompt) {
            if (turningPoints.isEmpty()) {
                return;
            }

            prompt.append("[Turning Points]\n");
            for (TurningPoint turningPoint : turningPoints) {
                prompt.append("- ");
                appendInline(prompt, "Event", turningPoint.getEventType());
                appendInline(prompt, "Impact Emotion", turningPoint.getImpactEmotion());
                appendInline(prompt, "Impact Score", turningPoint.getImpactScore());
                appendInline(prompt, "Summary", turningPoint.getSummary());
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
