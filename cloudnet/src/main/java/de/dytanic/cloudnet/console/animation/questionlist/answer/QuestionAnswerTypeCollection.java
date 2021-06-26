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

import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeCollection implements QuestionAnswerType<Collection<String>> {

  private Collection<String> possibleAnswers;
  private boolean allowEmpty = true;

  public QuestionAnswerTypeCollection(Collection<String> possibleAnswers) {
    this.possibleAnswers = possibleAnswers;
  }

  public QuestionAnswerTypeCollection() {
  }

  public QuestionAnswerTypeCollection disallowEmpty() {
    this.allowEmpty = false;
    return this;
  }

  @Override
  public boolean isValidInput(@NotNull String input) {
    if (!this.allowEmpty && input.trim().isEmpty()) {
      return false;
    }
    return this.possibleAnswers == null || Arrays.stream(input.split(";"))
      .allMatch(entry -> this.possibleAnswers.contains(entry));
  }

  @Override
  public @NotNull Collection<String> parse(@NotNull String input) {
    return new ArrayList<>(Arrays.asList(input.split(";")));
  }

  @Override
  public List<String> getCompletableAnswers() {
    return this.possibleAnswers != null ? new ArrayList<>(this.possibleAnswers) : null;
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return this.possibleAnswers;
  }

}
