package de.dytanic.cloudnet.command.sub;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;

public class SubCommandArgument<T> {
    private final QuestionAnswerType<T> answerType;
    private final T answer;

    public SubCommandArgument(QuestionAnswerType<T> answerType, T answer) {
        this.answerType = answerType;
        this.answer = answer;
    }

    public QuestionAnswerType<T> getAnswerType() {
        return this.answerType;
    }

    public T getAnswer() {
        return this.answer;
    }
}
