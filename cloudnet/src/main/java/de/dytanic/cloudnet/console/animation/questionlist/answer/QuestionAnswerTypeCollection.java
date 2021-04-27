package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class QuestionAnswerTypeCollection implements QuestionAnswerType<Collection<String>> {

    private Collection<String> possibleAnswers;
    private boolean allowEmpty = true;

    public QuestionAnswerTypeCollection(Collection<String> possibleAnswers) {
        this.possibleAnswers = possibleAnswers;
    }

    public QuestionAnswerTypeCollection() {
    }

    public QuestionAnswerTypeCollection disallowEmpty() {
        this.allowEmpty = false;
        return this;
    }

    @Override
    public boolean isValidInput(@NotNull String input) {
        if (!this.allowEmpty && input.trim().isEmpty()) {
            return false;
        }
        return this.possibleAnswers == null || Arrays.stream(input.split(";")).allMatch(entry -> this.possibleAnswers.contains(entry));
    }

    @Override
    public @NotNull Collection<String> parse(@NotNull String input) {
        return new ArrayList<>(Arrays.asList(input.split(";")));
    }

    @Override
    public List<String> getCompletableAnswers() {
        return this.possibleAnswers != null ? new ArrayList<>(this.possibleAnswers) : null;
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return this.possibleAnswers;
    }

}
