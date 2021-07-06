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

package de.dytanic.cloudnet.command.sub;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeStaticStringArray implements QuestionAnswerType<String> {

  private final String[] allowedValues;
  private final boolean ignoreCase;

  public QuestionAnswerTypeStaticStringArray(String[] allowedValues, boolean ignoreCase) {
    Preconditions.checkArgument(allowedValues.length > 0, "At least one value has to be provided");
    this.allowedValues = allowedValues;
    this.ignoreCase = ignoreCase;
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    return Arrays.stream(this.allowedValues)
      .anyMatch(value -> (this.ignoreCase && value.equalsIgnoreCase(input)) || value.equals(input));
  }

  @Override
  public @NotNull String parse(@NotNull String input) {
    return Arrays.stream(this.allowedValues).filter(value -> value.equalsIgnoreCase(input)).findFirst()
      .orElseThrow(() -> new IllegalStateException("Calling parse when isValidInput was false"));
  }

  @Override
  public String getInvalidInputMessage(@NotNull String input) {
    return null;
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return Collections.singletonList(this.allowedValues[0]);
  }
}
