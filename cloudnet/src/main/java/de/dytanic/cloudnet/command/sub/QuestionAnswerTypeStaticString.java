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

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeStaticString implements QuestionAnswerType<String> {

  private final String requiredValue;
  private final boolean ignoreCase;

  public QuestionAnswerTypeStaticString(String requiredValue, boolean ignoreCase) {
    this.requiredValue = requiredValue;
    this.ignoreCase = ignoreCase;
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    return (this.ignoreCase && input.equalsIgnoreCase(this.requiredValue)) || input.equals(this.requiredValue);
  }

  @Override
  public @NotNull String parse(@NotNull String input) {
    return this.requiredValue;
  }

  @Override
  public String getInvalidInputMessage(@NotNull String input) {
    return null;
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return Collections.singletonList(this.requiredValue);
  }
}
