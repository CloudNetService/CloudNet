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

package de.dytanic.cloudnet.console.animation.setup.answer;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.language.I18n;
import org.jetbrains.annotations.NotNull;

public final class QuestionListEntry<T> {

  private final String key;
  private final String question;
  private final QuestionAnswerType<T> answerType;

  private QuestionListEntry(@NotNull String key, @NotNull String question, @NotNull QuestionAnswerType<T> answerType) {
    this.key = key;
    this.question = question;
    this.answerType = answerType;
  }

  public static @NotNull <T> Builder<T> builder() {
    return new Builder<>();
  }

  public @NotNull String getKey() {
    return this.key;
  }

  public @NotNull String getQuestion() {
    return this.question;
  }

  public @NotNull QuestionAnswerType<T> getAnswerType() {
    return this.answerType;
  }

  public static final class Builder<T> {

    private String key;
    private String question;
    private QuestionAnswerType<T> answerType;

    public @NotNull Builder<T> key(@NotNull String key) {
      this.key = key;
      return this;
    }

    public @NotNull Builder<T> translatedQuestion(@NotNull String questionTranslationKey) {
      return this.question(I18n.trans(questionTranslationKey));
    }

    public @NotNull Builder<T> question(@NotNull String question) {
      this.question = question;
      return this;
    }

    public @NotNull Builder<T> answerType(@NotNull QuestionAnswerType.Builder<T> type) {
      return this.answerType(type.build());
    }

    public @NotNull Builder<T> answerType(@NotNull QuestionAnswerType<T> type) {
      this.answerType = type;
      return this;
    }

    public @NotNull QuestionListEntry<T> build() {
      Verify.verifyNotNull(this.key, "no key given");
      Verify.verifyNotNull(this.question, "no question given");
      Verify.verifyNotNull(this.answerType, "no answer type given");

      return new QuestionListEntry<>(this.key, this.question, this.answerType);
    }
  }
}
