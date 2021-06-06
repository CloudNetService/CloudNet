package de.dytanic.cloudnet.console.animation.questionlist;

import org.jetbrains.annotations.NotNull;

public class QuestionListEntry<T> {

  private final String key;
  private final String question;
  private final QuestionAnswerType<T> answerType;

  public QuestionListEntry(String key, String question, QuestionAnswerType<T> answerType) {
    this.key = key;
    this.question = question;
    this.answerType = answerType;
  }

  public @NotNull String getKey() {
    return this.key;
  }

  public @NotNull String getQuestion() {
    return this.question;
  }

  public @NotNull QuestionAnswerType<T> getAnswerType() {
    return this.answerType;
  }
}
