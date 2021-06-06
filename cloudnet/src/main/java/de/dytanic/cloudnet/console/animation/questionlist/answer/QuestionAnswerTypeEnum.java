package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeEnum<E extends Enum<E>> implements QuestionAnswerType<E> {

  private final Class<E> enumClass;

  public QuestionAnswerTypeEnum(Class<E> enumClass) {
    this.enumClass = enumClass;
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    return Arrays.stream(this.values()).anyMatch(e -> e.name().equalsIgnoreCase(input));
  }

  @Override
  public @NotNull E parse(@NotNull String input) {
    return Arrays.stream(this.values()).filter(e -> e.name().equalsIgnoreCase(input)).findFirst()
      .orElseThrow(() -> new IllegalStateException("Calling parse when isValidInput was false"));
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return this.getCompletableAnswers();
  }

  @Override
  public List<String> getCompletableAnswers() {
    return Arrays.stream(this.values()).map(Enum::name).collect(Collectors.toList());
  }

  protected E[] values() {
    return this.enumClass.getEnumConstants();
  }

}
