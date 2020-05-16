package de.dytanic.cloudnet.console.animation.questionlist;

public class QuestionListEntry<T> {
    private final String key;
    private final String question;
    private final QuestionAnswerType<T> answerType;

    public QuestionListEntry(String key, String question, QuestionAnswerType<T> answerType) {
        this.key = key;
        this.question = question;
        this.answerType = answerType;
    }

    public String getKey() {
        return this.key;
    }

    public String getQuestion() {
        return this.question;
    }

    public QuestionAnswerType<T> getAnswerType() {
        return this.answerType;
    }
}
