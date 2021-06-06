package de.dytanic.cloudnet.console.animation.questionlist.answer;

import de.dytanic.cloudnet.common.language.LanguageManager;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeIntRange extends QuestionAnswerTypeInt {

  private final int minValue;
  private final int maxValue;

  public QuestionAnswerTypeIntRange(int minValue, int maxValue) {
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    try {
      int value = Integer.parseInt(input);
      return value >= this.minValue && value <= this.maxValue;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  @Override
  public @NotNull Integer parse(@NotNull String input) {
    return Integer.parseInt(input);
  }

  @Override
  public String getPossibleAnswersAsString() {
    return "[" + this.minValue + ", " + this.maxValue + "]";
  }

  @Override
  public String getInvalidInputMessage(@NotNull String input) {
    return LanguageManager.getMessage("ca-question-list-invalid-int-range")
      .replace("%min%", String.valueOf(this.minValue))
      .replace("%max%", String.valueOf(this.maxValue));
  }

}
