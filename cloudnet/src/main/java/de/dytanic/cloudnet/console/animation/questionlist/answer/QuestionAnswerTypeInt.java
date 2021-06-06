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

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionAnswerType;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class QuestionAnswerTypeInt implements QuestionAnswerType<Integer> {

  @Override
  public boolean isValidInput(@NotNull String input) {
    return Ints.tryParse(input) != null;
  }

  @Override
  public @NotNull Integer parse(@NotNull String input) {
    return Integer.parseInt(input);
  }

  @Override
  public Collection<String> getPossibleAnswers() {
    return null;
  }

  @Override
  public String getInvalidInputMessage(@NotNull String input) {
    return LanguageManager.getMessage("ca-question-list-invalid-int");
  }

}
