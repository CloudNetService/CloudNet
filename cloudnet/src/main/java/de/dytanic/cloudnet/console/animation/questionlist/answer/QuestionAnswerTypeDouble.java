package de.dytanic.cloudnet.console.animation.questionlist.answer;

import com.google.common.primitives.Doubles;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

import java.util.Collection;

public class QuestionAnswerTypeDouble implements QuestionAnswerType<Double> {

    @Override
    public boolean isValidInput(String input) {
        return Doubles.tryParse(input) != null;
    }

    @Override
    public Double parse(String input) {
        return Double.parseDouble(input);
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

    @Override
    public String getInvalidInputMessage(String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-double");
    }

}
