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

public class SubCommandArgument<T> {

  private final QuestionAnswerType<T> answerType;
  private final T answer;

  public SubCommandArgument(QuestionAnswerType<T> answerType, T answer) {
    this.answerType = answerType;
    this.answer = answer;
  }

  public QuestionAnswerType<T> getAnswerType() {
    return this.answerType;
  }

  public T getAnswer() {
    return this.answer;
  }
}
