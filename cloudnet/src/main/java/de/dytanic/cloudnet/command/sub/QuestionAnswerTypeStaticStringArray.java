package de.dytanic.cloudnet.command.sub;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class QuestionAnswerTypeStaticStringArray implements QuestionAnswerType<String> {

    private final String[] allowedValues;
    private final boolean ignoreCase;

    public QuestionAnswerTypeStaticStringArray(String[] allowedValues, boolean ignoreCase) {
        Preconditions.checkArgument(allowedValues.length > 0, "At least one value has to be provided");
        this.allowedValues = allowedValues;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public boolean isValidInput(@NotNull String input) {
        return Arrays.stream(this.allowedValues)
                .anyMatch(value -> (this.ignoreCase && value.equalsIgnoreCase(input)) || value.equals(input));
    }

    @Override
    public @NotNull String parse(@NotNull String input) {
        return Arrays.stream(this.allowedValues).filter(value -> value.equalsIgnoreCase(input)).findFirst().orElseThrow(() -> new IllegalStateException("Calling parse when isValidInput was false"));
    }

    @Override
    public String getInvalidInputMessage(@NotNull String input) {
        return null;
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return Collections.singletonList(this.allowedValues[0]);
    }
}
