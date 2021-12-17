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

package de.dytanic.cloudnet.common.document.property;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.document.IDocument;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public record FunctionalDocProperty<E>(
  @NonNull Function<IDocument<?>, E> reader,
  @NonNull BiConsumer<E, IDocument<?>> writer,
  @NonNull Consumer<IDocument<?>> remover,
  @NonNull Predicate<IDocument<?>> containsTester
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
  public void remove(@NonNull IDocument<?> from) {
    this.remover.accept(from);
  }

  @Override
  public void append(@NonNull IDocument<?> to, @Nullable E value) {
    this.writer.accept(value, to);
  }

  @Override
  public @UnknownNullability E get(@NonNull IDocument<?> from) {
    return this.reader.apply(from);
  }

  @Override
  public boolean isAppendedTo(@NonNull IDocument<?> document) {
    return this.containsTester.test(document);
  }

  public static class Builder<E> {

    private Function<IDocument<?>, E> reader;
    private BiConsumer<E, IDocument<?>> writer;

    private Consumer<IDocument<?>> remover;
    private Predicate<IDocument<?>> containsTester;

    public @NonNull Builder<E> reader(@NonNull Function<IDocument<?>, E> reader) {
      this.reader = reader;
      return this;
    }

    public @NonNull Builder<E> writer(@NonNull BiConsumer<E, IDocument<?>> writer) {
      this.writer = writer;
      return this;
    }

    public @NonNull Builder<E> remover(@NonNull Consumer<IDocument<?>> remover) {
      this.remover = remover;
      return this;
    }

    public @NonNull Builder<E> containsTester(@NonNull Predicate<IDocument<?>> containsTester) {
      this.containsTester = containsTester;
      return this;
    }

    public @NonNull DocProperty<E> build() {
      Verify.verifyNotNull(this.reader, "no reader given");
      Verify.verifyNotNull(this.writer, "no writer given");
      Verify.verifyNotNull(this.reader, "no remover given");
      Verify.verifyNotNull(this.containsTester, "no contains tester given");

      return new FunctionalDocProperty<>(
        this.reader,
        this.writer,
        this.remover,
        this.containsTester);
    }
  }
}
