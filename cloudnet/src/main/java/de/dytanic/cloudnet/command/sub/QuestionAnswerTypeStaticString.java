package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

import java.util.Collection;
import java.util.Collections;

public class QuestionAnswerTypeStaticString implements QuestionAnswerType<String> {

    private final String requiredValue;
    private final boolean ignoreCase;

    public QuestionAnswerTypeStaticString(String requiredValue, boolean ignoreCase) {
        this.requiredValue = requiredValue;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public boolean isValidInput(String input) {
        return (this.ignoreCase && input.equalsIgnoreCase(this.requiredValue)) || input.equals(this.requiredValue);
    }

    @Override
    public String parse(String input) {
        return this.requiredValue;
    }

    @Override
    public String getInvalidInputMessage(String input) {
        return null;
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return Collections.singletonList(this.requiredValue);
    }
}
