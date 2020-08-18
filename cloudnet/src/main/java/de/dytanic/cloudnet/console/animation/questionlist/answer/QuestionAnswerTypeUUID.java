package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

public class QuestionAnswerTypeUUID implements QuestionAnswerType<UUID> {

    @Override
    public boolean isValidInput(@NotNull String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public @NotNull UUID parse(@NotNull String input) {
        return UUID.fromString(input);
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

    @Override
    public String getInvalidInputMessage(@NotNull String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-uuid");
    }
}
