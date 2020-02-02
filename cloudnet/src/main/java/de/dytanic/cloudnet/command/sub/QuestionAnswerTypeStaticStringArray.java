package de.dytanic.cloudnet.command.sub;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class QuestionAnswerTypeStaticStringArray implements QuestionAnswerType<String> {

    private String[] allowedValues;
    private boolean ignoreCase;

    public QuestionAnswerTypeStaticStringArray(String[] allowedValues, boolean ignoreCase) {
        Preconditions.assertFalse(allowedValues.length == 0, "At least one value has to be provided");
        this.allowedValues = allowedValues;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public boolean isValidInput(String input) {
        return Arrays.stream(this.allowedValues)
                .anyMatch(value -> (this.ignoreCase && value.equalsIgnoreCase(input)) || value.equals(input));
    }

    @Override
    public String parse(String input) {
        return Arrays.stream(this.allowedValues).filter(value -> value.equalsIgnoreCase(input)).findFirst().get();
    }

    @Override
    public String getInvalidInputMessage(String input) {
        return null;
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return Collections.singletonList(this.allowedValues[0]);
    }
}
