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
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class SubCommandArgumentWrapper {

  private final SubCommandArgument<?>[] arguments;

  public SubCommandArgumentWrapper(SubCommandArgument<?>[] arguments) {
    this.arguments = arguments;
  }

  public SubCommandArgumentWrapper(Collection<SubCommandArgument<?>> arguments) {
    this(arguments.toArray(new SubCommandArgument[0]));
  }

  public Object[] array() {
    Object[] objects = new Object[this.arguments.length];
    for (int i = 0; i < this.arguments.length; i++) {
      objects[i] = this.arguments[i].getAnswer();
    }
    return objects;
  }

  public int length() {
    return this.arguments.length;
  }

  public QuestionAnswerType<?> argumentType(int index) {
    return this.hasArgument(index) ? this.arguments[index].getAnswerType() : null;
  }

  public Object argument(int index) {
    return this.hasArgument(index) ? this.arguments[index].getAnswer() : null;
  }

  public boolean hasArgument(int index) {
    return this.arguments.length > index && index >= 0;
  }


  private Stream<SubCommandArgument<?>> argumentStream(String key) {
    return Arrays.stream(this.arguments)
      .filter(argument -> argument.getAnswerType().getRecommendation() != null)
      .filter(argument -> argument.getAnswerType().getRecommendation().equalsIgnoreCase(key));
  }

  public Optional<QuestionAnswerType<?>> argumentType(String key) {
    return this.argumentStream(key)
      .findFirst()
      .map(SubCommandArgument::getAnswerType);
  }

  public Optional<Object> argument(String key) {
    return this.argumentStream(key)
      .findFirst()
      .map(SubCommandArgument::getAnswer);
  }

  public Optional<Object> argument(Class<? extends QuestionAnswerType> answerTypeClass) {
    return Arrays.stream(this.arguments)
      .filter(argument -> answerTypeClass.isAssignableFrom(argument.getAnswerType().getClass()))
      .findFirst()
      .map(SubCommandArgument::getAnswer);
  }

  public boolean hasArgument(String key) {
    return this.argument(key).isPresent();
  }


}
