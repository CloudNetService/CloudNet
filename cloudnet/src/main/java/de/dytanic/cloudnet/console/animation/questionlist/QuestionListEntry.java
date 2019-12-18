package de.dytanic.cloudnet.console.animation.questionlist;

public class QuestionListEntry<T> {
    private String key;
    private String question;
    private QuestionAnswerType<T> answerType;

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
