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
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeBoolean implements QuestionAnswerType<Boolean> {

  private final String trueString;
  private final String falseString;

  public QuestionAnswerTypeBoolean(String trueString, String falseString) {
    this.trueString = trueString != null ? trueString : "yes";
    this.falseString = falseString != null ? falseString : "no";
  }

  public QuestionAnswerTypeBoolean() {
    this(LanguageManager.getMessage("ca-question-list-boolean-true"),
      LanguageManager.getMessage("ca-question-list-boolean-false"));
  }

  public String getTrueString() {
    return this.trueString;
  }

  public String getFalseString() {
    return this.falseString;
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    return this.trueString.equalsIgnoreCase(input) || this.falseString.equalsIgnoreCase(input);
  }

  @Override
  public @NotNull Boolean parse(@NotNull String input) {
    return this.trueString.equalsIgnoreCase(input);
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return this.getCompletableAnswers();
  }

  @Override
  public List<String> getCompletableAnswers() {
    return Arrays.asList(this.trueString, this.falseString);
  }

  @Override
  public String getInvalidInputMessage(@NotNull String input) {
    return LanguageManager.getMessage("ca-question-list-invalid-boolean")
      .replace("%true%", this.trueString)
      .replace("%false%", this.falseString);
  }

}
