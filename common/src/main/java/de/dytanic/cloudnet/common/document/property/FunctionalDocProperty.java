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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class FunctionalDocProperty<E> implements DocProperty<E> {

  private final Function<IDocument<?>, E> reader;
  private final BiConsumer<E, IDocument<?>> writer;

  private final Consumer<IDocument<?>> remover;
  private final Predicate<IDocument<?>> containsTester;

  public FunctionalDocProperty(
    @NotNull Function<IDocument<?>, E> reader,
    @NotNull BiConsumer<E, IDocument<?>> writer,
    @NotNull Consumer<IDocument<?>> remover,
    @NotNull Predicate<IDocument<?>> containsTester
  ) {
    this.reader = reader;
    this.writer = writer;
    this.remover = remover;
    this.containsTester = containsTester;
  }

  public static @NotNull <E> Builder<E> builder() {
    return new Builder<>();
  }

  public static @NotNull <E> Builder<E> forNamedProperty(@NotNull String propertyName) {
    return FunctionalDocProperty.<E>builder()
      .remover(document -> document.remove(propertyName))
      .containsTester(document -> document.contains(propertyName));
  }

  @Override
  public void remove(@NotNull IDocument<?> from) {
    this.remover.accept(from);
  }

  @Override
  public void append(@NotNull IDocument<?> to, @Nullable E value) {
    this.writer.accept(value, to);
  }

  @Override
  public @UnknownNullability E get(@NotNull IDocument<?> from) {
    return this.reader.apply(from);
  }

  @Override
  public boolean isAppendedTo(@NotNull IDocument<?> document) {
    return this.containsTester.test(document);
  }

  public static class Builder<E> {

    private Function<IDocument<?>, E> reader;
    private BiConsumer<E, IDocument<?>> writer;

    private Consumer<IDocument<?>> remover;
    private Predicate<IDocument<?>> containsTester;

    public @NotNull Builder<E> reader(@NotNull Function<IDocument<?>, E> reader) {
      this.reader = reader;
      return this;
    }

    public @NotNull Builder<E> writer(@NotNull BiConsumer<E, IDocument<?>> writer) {
      this.writer = writer;
      return this;
    }

    public @NotNull Builder<E> remover(@NotNull Consumer<IDocument<?>> remover) {
      this.remover = remover;
      return this;
    }

    public @NotNull Builder<E> containsTester(@NotNull Predicate<IDocument<?>> containsTester) {
      this.containsTester = containsTester;
      return this;
    }

    public @NotNull DocProperty<E> build() {
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
