package de.dytanic.cloudnet.console.animation.questionlist.answer;

import com.google.common.primitives.Doubles;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class QuestionAnswerTypeDouble implements QuestionAnswerType<Double> {

    @Override
    public boolean isValidInput(@NotNull String input) {
        return Doubles.tryParse(input) != null;
    }

    @Override
    public @NotNull Double parse(@NotNull String input) {
        return Double.parseDouble(input);
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

    @Override
    public String getInvalidInputMessage(@NotNull String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-double");
    }

}
