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
