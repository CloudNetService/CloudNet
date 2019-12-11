package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionAnswerTypeEnum<E extends Enum<E>> implements QuestionAnswerType<E> {

    private Class<E> enumClass;

    public QuestionAnswerTypeEnum(Class<E> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public boolean isValidInput(String input) {
        return Arrays.stream(this.enumClass.getEnumConstants()).anyMatch(e -> e.name().equalsIgnoreCase(input));
    }

    @Override
    public E parse(String input) {
        return Arrays.stream(this.enumClass.getEnumConstants()).filter(e -> e.name().equalsIgnoreCase(input)).findFirst().get();
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return this.getCompletableAnswers();
    }

    @Override
    public List<String> getCompletableAnswers() {
        return Arrays.stream(this.enumClass.getEnumConstants()).map(Enum::name).collect(Collectors.toList());
    }
}
