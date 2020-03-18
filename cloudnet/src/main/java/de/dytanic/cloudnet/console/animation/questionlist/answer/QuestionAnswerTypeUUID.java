package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

import java.util.Collection;
import java.util.UUID;

public class QuestionAnswerTypeUUID implements QuestionAnswerType<UUID> {

    @Override
    public boolean isValidInput(String input) {
        return parse(input) != null;
    }

    @Override
    public UUID parse(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

    @Override
    public String getInvalidInputMessage(String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-uuid");
    }
}
