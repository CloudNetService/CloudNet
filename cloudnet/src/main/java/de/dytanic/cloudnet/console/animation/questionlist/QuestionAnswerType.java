package de.dytanic.cloudnet.console.animation.questionlist;

import de.dytanic.cloudnet.common.language.LanguageManager;

import java.util.Collection;
import java.util.List;

public interface QuestionAnswerType<T> {

    boolean isValidInput(String input);

    T parse(String input);

    //null response here means that we have infinity possible answers
    Collection<String> getPossibleAnswers();

    default List<String> getCompletableAnswers() {
        return null;
    }

    default String getRecommendation() {
        return null;
    }

    default String getPossibleAnswersAsString() {
        Collection<String> possibleAnswers = this.getPossibleAnswers();
        return possibleAnswers != null ? String.join(", ", possibleAnswers) : null;
    }

    default String getInvalidInputMessage(String input) {
        Collection<String> possibleAnswers = this.getPossibleAnswers();
        if (possibleAnswers != null) {
            return LanguageManager.getMessage("ca-question-list-question-list").replace("%values%", this.getPossibleAnswersAsString());
        }
        return LanguageManager.getMessage("ca-question-list-default");
    }

}
