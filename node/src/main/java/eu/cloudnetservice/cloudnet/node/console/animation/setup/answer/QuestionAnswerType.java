/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.node.console.animation.setup.answer;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public record QuestionAnswerType<T>(
  @NonNull Parser<T> parser,
  @Nullable String recommendation,
  @NonNull Supplier<Collection<String>> possibleResults,
  @NonNull Collection<BiConsumer<QuestionAnswerType<T>, T>> resultListener,
  @NonNull BiFunction<QuestionAnswerType<?>, String, String> invalidInputSupplier
) {

  private static final BiFunction<QuestionAnswerType<?>, String, String> DEFAULT_INVALID_SUPPLIER = (type, $) -> {
    // check if there are possible results
    if (type.possibleAnswers().isEmpty()) {
      return I18n.trans("ca-question-list-invalid-default");
    } else {
      return I18n.trans("ca-question-list-question-list", String.join(", ", type.possibleAnswers()));
    }
  };

  public static @NonNull <T> Builder<T> builder() {
    return new Builder<>();
  }

  @Unmodifiable
  public @NonNull Collection<String> possibleAnswers() {
    return this.possibleResults.get();
  }

  public @NonNull String invalidInputMessage(@NonNull String inputLine) {
    return this.invalidInputSupplier.apply(this, inputLine);
  }

  public @Nullable T tryParse(@NonNull String inputLine) throws Exception {
    return this.parser.parse(inputLine);
  }

  @SuppressWarnings("unchecked")
  public void postResult(@Nullable Object result) {
    for (var listener : this.resultListener) {
      listener.accept(this, (T) result);
    }
  }

  public void thenAccept(@NonNull BiConsumer<QuestionAnswerType<T>, T> listener) {
    this.resultListener.add(listener);
  }

  @FunctionalInterface
  public interface Parser<T> {

    @Nullable T parse(@NonNull String input) throws Exception;
  }

  public static final class Builder<T> {

    private Parser<T> parser;
    private String recommendation;
    private Supplier<Collection<String>> possibleResults;
    private Collection<BiConsumer<QuestionAnswerType<T>, T>> resultListener;
    private BiFunction<QuestionAnswerType<?>, String, String> invalidInputSupplier;

    public @NonNull Builder<T> parser(@NonNull Parser<T> parser) {
      this.parser = parser;
      return this;
    }

    public @NonNull Builder<T> recommendation(@NonNull Object recommendation) {
      return this.recommendation(String.valueOf(recommendation));
    }

    public @NonNull Builder<T> recommendation(@NonNull String recommendation) {
      this.recommendation = recommendation;
      return this;
    }

    public @NonNull Builder<T> possibleResults(@NonNull Collection<String> results) {
      this.possibleResults = () -> new ArrayList<>(results);
      return this;
    }

    public @NonNull Builder<T> possibleResults(@NonNull Supplier<Collection<String>> results) {
      this.possibleResults = results;
      return this;
    }

    public @NonNull Builder<T> possibleResults(String @NonNull ... results) {
      return this.possibleResults(Arrays.asList(results));
    }

    public @NonNull Builder<T> resultListener(@NonNull Collection<BiConsumer<QuestionAnswerType<T>, T>> listeners) {
      this.resultListener = new ArrayList<>(listeners);
      return this;
    }

    public @NonNull Builder<T> addResultListener(@NonNull BiConsumer<QuestionAnswerType<T>, T> listener) {
      if (this.resultListener == null) {
        return this.resultListener(Collections.singleton(listener));
      } else {
        this.resultListener.add(listener);
        return this;
      }
    }

    public @NonNull Builder<T> invalidInputHandler(@NonNull BiFunction<QuestionAnswerType<?>, String, String> handler) {
      this.invalidInputSupplier = handler;
      return this;
    }

    public @NonNull Builder<T> invalidInputMessage(@NonNull String message) {
      return this.invalidInputHandler((__, ___) -> message);
    }

    public @NonNull Builder<T> translatedInvalidInputMessage(@NonNull String messageTranslationKey) {
      return this.invalidInputHandler((__, ___) -> I18n.trans(messageTranslationKey));
    }

    public @NonNull QuestionAnswerType<T> build() {
      Preconditions.checkNotNull(this.parser, "no parser given");
      return new QuestionAnswerType<>(
        this.parser,
        this.recommendation,
        this.possibleResults == null ? Collections::emptyList : this.possibleResults,
        this.resultListener == null ? new ArrayList<>() : this.resultListener,
        this.invalidInputSupplier == null ? DEFAULT_INVALID_SUPPLIER : this.invalidInputSupplier);
    }
  }
}
