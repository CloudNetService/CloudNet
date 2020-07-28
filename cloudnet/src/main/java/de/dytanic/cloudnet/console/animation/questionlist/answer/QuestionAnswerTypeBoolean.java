package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class QuestionAnswerTypeBoolean implements QuestionAnswerType<Boolean> {
    private final String trueString;
    private final String falseString;

    public QuestionAnswerTypeBoolean(String trueString, String falseString) {
        this.trueString = trueString != null ? trueString : "yes";
        this.falseString = falseString != null ? falseString : "no";
    }

    public QuestionAnswerTypeBoolean() {
        this(LanguageManager.getMessage("ca-question-list-boolean-true"), LanguageManager.getMessage("ca-question-list-boolean-false"));
    }

    public String getTrueString() {
        return this.trueString;
    }

    public String getFalseString() {
        return this.falseString;
    }

    @Override
    public boolean isValidInput(@NotNull String input) {
        return this.trueString.equalsIgnoreCase(input) || this.falseString.equalsIgnoreCase(input);
    }

    @Override
    public @NotNull Boolean parse(@NotNull String input) {
        return this.trueString.equalsIgnoreCase(input);
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return this.getCompletableAnswers();
    }

    @Override
    public List<String> getCompletableAnswers() {
        return Arrays.asList(this.trueString, this.falseString);
    }

    @Override
    public String getInvalidInputMessage(@NotNull String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-boolean")
                .replace("%true%", this.trueString)
                .replace("%false%", this.falseString);
    }

}
