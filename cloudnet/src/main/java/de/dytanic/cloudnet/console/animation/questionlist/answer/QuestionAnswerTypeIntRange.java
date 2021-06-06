/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
