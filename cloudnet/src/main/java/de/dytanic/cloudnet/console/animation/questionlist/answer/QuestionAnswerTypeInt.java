package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

import java.util.Collection;

public class QuestionAnswerTypeInt implements QuestionAnswerType<Integer> {
    @Override
    public boolean isValidInput(String input) {
        return Validate.testStringParseToInt(input);
    }

    @Override
    public Integer parse(String input) {
        return Integer.parseInt(input);
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

    @Override
    public String getInvalidInputMessage(String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-int");
    }
}
