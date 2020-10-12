package de.dytanic.cloudnet.console.animation.questionlist;

import de.dytanic.cloudnet.common.language.LanguageManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface QuestionAnswerType<T> {

    boolean isValidInput(@NotNull String input);

    @Nullable T parse(@NotNull String input);

    /**
     * @return null if there are infinite possible numbers
     */
    @Nullable Collection<String> getPossibleAnswers();

    default @Nullable List<String> getCompletableAnswers() {
        return null;
    }

    default @Nullable String getRecommendation() {
        return null;
    }

    default @Nullable String getPossibleAnswersAsString() {
        Collection<String> possibleAnswers = this.getPossibleAnswers();
        return possibleAnswers != null ? String.join(", ", possibleAnswers) : null;
    }

    default @Nullable String getInvalidInputMessage(@NotNull String input) {
        String s = this.getPossibleAnswersAsString();
        if (s != null) {
            return LanguageManager.getMessage("ca-question-list-question-list").replace("%values%", s);
        }

        return LanguageManager.getMessage("ca-question-list-invalid-default");
    }
}
