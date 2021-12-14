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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public final class QuestionAnswerType<T> {

  private static final BiFunction<QuestionAnswerType<?>, String, String> DEFAULT_INVALID_SUPPLIER = (type, $) -> {
    // check if there are possible results
    if (type.getPossibleAnswers().isEmpty()) {
      return I18n.trans("ca-question-list-invalid-default");
    } else {
      return I18n.trans("ca-question-list-question-list")
        .replace("%values%", String.join(", ", type.getPossibleAnswers()));
    }
  };

  private final Parser<T> parser;
  private final String recommendation;
  private final Supplier<Collection<String>> possibleResults;
  private final Collection<BiConsumer<QuestionAnswerType<T>, T>> resultListener;
  private final BiFunction<QuestionAnswerType<?>, String, String> invalidInputSupplier;

  private QuestionAnswerType(
    @NotNull Parser<T> parser,
    @Nullable String recommendation,
    @NotNull Supplier<Collection<String>> possibleResults,
    @NotNull Collection<BiConsumer<QuestionAnswerType<T>, T>> resultListener,
    @NotNull BiFunction<QuestionAnswerType<?>, String, String> invalidInputSupplier
  ) {
    this.parser = parser;
    this.recommendation = recommendation;
    this.possibleResults = possibleResults;
    this.resultListener = resultListener;
    this.invalidInputSupplier = invalidInputSupplier;
  }

  public static @NotNull <T> Builder<T> builder() {
    return new Builder<>();
  }

  @Unmodifiable
  public @NotNull Collection<String> getPossibleAnswers() {
    return this.possibleResults.get();
  }

  public @Nullable String getRecommendation() {
    return this.recommendation;
  }

  public @NotNull String getInvalidInputMessage(@NotNull String inputLine) {
    return this.invalidInputSupplier.apply(this, inputLine);
  }

  public @Nullable T tryParse(@NotNull String inputLine) throws Exception {
    return this.parser.parse(inputLine);
  }

  @SuppressWarnings("unchecked")
  public void postResult(@Nullable Object result) {
    for (var listener : this.resultListener) {
      listener.accept(this, (T) result);
    }
  }

  public void thenAccept(@NotNull BiConsumer<QuestionAnswerType<T>, T> listener) {
    this.resultListener.add(listener);
  }

  @FunctionalInterface
  public interface Parser<T> {

    @Nullable T parse(@NotNull String input) throws Exception;
  }

  public static final class Builder<T> {

    private Parser<T> parser;
    private String recommendation;
    private Supplier<Collection<String>> possibleResults;
    private Collection<BiConsumer<QuestionAnswerType<T>, T>> resultListener;
    private BiFunction<QuestionAnswerType<?>, String, String> invalidInputSupplier;

    public @NotNull Builder<T> parser(@NotNull Parser<T> parser) {
      this.parser = parser;
      return this;
    }

    public @NotNull Builder<T> recommendation(@NotNull Object recommendation) {
      return this.recommendation(String.valueOf(recommendation));
    }

    public @NotNull Builder<T> recommendation(@NotNull String recommendation) {
      this.recommendation = recommendation;
      return this;
    }

    public @NotNull Builder<T> possibleResults(@NotNull Collection<String> results) {
      this.possibleResults = () -> new ArrayList<>(results);
      return this;
    }

    public @NotNull Builder<T> possibleResults(@NotNull Supplier<Collection<String>> results) {
      this.possibleResults = results;
      return this;
    }

    public @NotNull Builder<T> possibleResults(String @NotNull ... results) {
      return this.possibleResults(Arrays.asList(results));
    }

    public @NotNull Builder<T> resultListener(@NotNull Collection<BiConsumer<QuestionAnswerType<T>, T>> listeners) {
      this.resultListener = new ArrayList<>(listeners);
      return this;
    }

    public @NotNull Builder<T> addResultListener(@NotNull BiConsumer<QuestionAnswerType<T>, T> listener) {
      if (this.resultListener == null) {
        return this.resultListener(Collections.singleton(listener));
      } else {
        this.resultListener.add(listener);
        return this;
      }
    }

    public @NotNull Builder<T> invalidInputHandler(@NotNull BiFunction<QuestionAnswerType<?>, String, String> handler) {
      this.invalidInputSupplier = handler;
      return this;
    }

    public @NotNull Builder<T> invalidInputMessage(@NotNull String message) {
      return this.invalidInputHandler((__, ___) -> message);
    }

    public @NotNull Builder<T> translatedInvalidInputMessage(@NotNull String messageTranslationKey) {
      return this.invalidInputMessage(I18n.trans(messageTranslationKey));
    }

    public @NotNull QuestionAnswerType<T> build() {
      Verify.verifyNotNull(this.parser, "no parser given");
      return new QuestionAnswerType<>(
        this.parser,
        this.recommendation,
        this.possibleResults == null ? Collections::emptyList : this.possibleResults,
        this.resultListener == null ? new ArrayList<>() : this.resultListener,
        this.invalidInputSupplier == null ? DEFAULT_INVALID_SUPPLIER : this.invalidInputSupplier);
    }
  }
}
