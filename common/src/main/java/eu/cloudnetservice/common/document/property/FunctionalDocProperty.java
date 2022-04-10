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

package eu.cloudnetservice.common.document.property;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.common.document.Document;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public record FunctionalDocProperty<E>(
  @NonNull Function<Document<?>, E> reader,
  @NonNull BiConsumer<E, Document<?>> writer,
  @NonNull Consumer<Document<?>> remover,
  @NonNull Predicate<Document<?>> containsTester
) implements DocProperty<E> {

  public static @NonNull <E> Builder<E> builder() {
    return new Builder<>();
  }

  public static @NonNull <E> Builder<E> forNamedProperty(@NonNull String propertyName) {
    return FunctionalDocProperty.<E>builder()
      .remover(document -> document.remove(propertyName))
      .containsTester(document -> document.contains(propertyName));
  }

  @Override
  public void remove(@NonNull Document<?> from) {
    this.remover.accept(from);
  }

  @Override
  public void append(@NonNull Document<?> to, @Nullable E value) {
    this.writer.accept(value, to);
  }

  @Override
  public @UnknownNullability E get(@NonNull Document<?> from) {
    return this.reader.apply(from);
  }

  @Override
  public boolean isAppendedTo(@NonNull Document<?> document) {
    return this.containsTester.test(document);
  }

  public static class Builder<E> {

    private Function<Document<?>, E> reader;
    private BiConsumer<E, Document<?>> writer;

    private Consumer<Document<?>> remover;
    private Predicate<Document<?>> containsTester;

    public @NonNull Builder<E> reader(@NonNull Function<Document<?>, E> reader) {
      this.reader = reader;
      return this;
    }

    public @NonNull Builder<E> writer(@NonNull BiConsumer<E, Document<?>> writer) {
      this.writer = writer;
      return this;
    }

    public @NonNull Builder<E> remover(@NonNull Consumer<Document<?>> remover) {
      this.remover = remover;
      return this;
    }

    public @NonNull Builder<E> containsTester(@NonNull Predicate<Document<?>> containsTester) {
      this.containsTester = containsTester;
      return this;
    }

    public @NonNull DocProperty<E> build() {
      Preconditions.checkNotNull(this.reader, "no reader given");
      Preconditions.checkNotNull(this.writer, "no writer given");
      Preconditions.checkNotNull(this.reader, "no remover given");
      Preconditions.checkNotNull(this.containsTester, "no contains tester given");

      return new FunctionalDocProperty<>(
        this.reader,
        this.writer,
        this.remover,
        this.containsTester);
    }
  }
}
