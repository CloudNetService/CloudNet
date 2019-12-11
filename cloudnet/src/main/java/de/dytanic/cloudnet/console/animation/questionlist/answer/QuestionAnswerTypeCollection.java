package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionAnswerTypeCollection implements QuestionAnswerType<Collection<String>> {

    private Collection<String> possibleAnswers;

    public QuestionAnswerTypeCollection(Collection<String> possibleAnswers) {
        this.possibleAnswers = possibleAnswers;
    }

    @Override
    public boolean isValidInput(String input) {
        return Arrays.stream(input.split(";")).allMatch(entry -> this.possibleAnswers.contains(entry));
    }

    @Override
    public Collection<String> parse(String input) {
        return Arrays.asList(input.split(";"));
    }

    @Override
    public List<String> getCompletableAnswers() {
        return new ArrayList<>(this.possibleAnswers);
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return this.possibleAnswers;
    }
}
