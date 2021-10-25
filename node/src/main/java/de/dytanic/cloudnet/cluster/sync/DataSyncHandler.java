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

package de.dytanic.cloudnet.cluster.sync;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataSyncHandler<T> {

  private final String key;
  private final boolean alwaysForceApply;
  private final DataConverter<T> converter;

  private final Consumer<T> writer;
  private final UnaryOperator<T> currentGetter;
  private final Function<T, String> nameExtractor;
  private final Supplier<Collection<T>> dataCollector;

  protected DataSyncHandler(
    @NotNull String key,
    boolean alwaysForceApply,
    @NotNull DataConverter<T> converter,
    @NotNull Consumer<T> writer,
    @NotNull UnaryOperator<T> currentGetter,
    @NotNull Function<T, String> nameExtractor,
    @NotNull Supplier<Collection<T>> dataCollector
  ) {
    this.key = key;
    this.alwaysForceApply = alwaysForceApply;
    this.converter = converter;
    this.writer = writer;
    this.currentGetter = currentGetter;
    this.nameExtractor = nameExtractor;
    this.dataCollector = dataCollector;
  }

  public static <T> @NotNull Builder<T> builder() {
    return new Builder<>();
  }

  public @NotNull String getKey() {
    return this.key;
  }

  public boolean isAlwaysForceApply() {
    return this.alwaysForceApply;
  }

  @SuppressWarnings("unchecked")
  public @NotNull String getName(@NotNull Object obj) {
    return this.nameExtractor.apply((T) obj);
  }

  public @NotNull DataConverter<T> getConverter() {
    return this.converter;
  }

  @SuppressWarnings("unchecked")
  public void write(@NotNull Object data) {
    this.writer.accept((T) data);
  }

  @SuppressWarnings("unchecked")
  public void serialize(@NotNull DataBuf.Mutable target, @NotNull Object data) {
    this.converter.write(target, (T) data);
  }

  @SuppressWarnings("unchecked")
  public @Nullable T getCurrent(@NotNull Object toGet) {
    return this.currentGetter.apply((T) toGet);
  }

  public @NotNull Collection<T> getData() {
    return this.dataCollector.get();
  }

  public interface DataConverter<T> {

    void write(@NotNull DataBuf.Mutable target, @NotNull T data);

    @NotNull T parse(@NotNull DataBuf input) throws Exception;
  }

  public static final class Builder<T> {

    private String key;
    private boolean alwaysForceApply;
    private DataConverter<T> converter;

    private Consumer<T> writer;
    private UnaryOperator<T> currentGetter;
    private Function<T, String> nameExtractor;
    private Supplier<Collection<T>> dataCollector;

    public @NotNull Builder<T> key(@NotNull String key) {
      this.key = key;
      return this;
    }

    public @NotNull Builder<T> alwaysForce() {
      this.alwaysForceApply = true;
      return this;
    }

    public @NotNull Builder<T> converter(@NotNull DataConverter<T> converter) {
      this.converter = converter;
      return this;
    }

    public @NotNull Builder<T> convertObject(@NotNull Type type) {
      return this.converter(new DataConverter<T>() {
        @Override
        public void write(@NotNull DataBuf.Mutable target, @NotNull T data) {
          target.writeObject(data);
        }

        @Override
        public @NotNull T parse(@NotNull DataBuf input) {
          return input.readObject(type);
        }
      });
    }

    public @NotNull Builder<T> writer(@NotNull Consumer<T> writer) {
      this.writer = writer;
      return this;
    }

    public @NotNull Builder<T> currentGetter(@NotNull UnaryOperator<T> currentGetter) {
      this.currentGetter = currentGetter;
      return this;
    }

    public @NotNull Builder<T> nameExtractor(@NotNull Function<T, String> nameExtractor) {
      this.nameExtractor = nameExtractor;
      return this;
    }

    public @NotNull Builder<T> dataCollector(@NotNull Supplier<Collection<T>> dataCollector) {
      this.dataCollector = dataCollector;
      return this;
    }

    public @NotNull Builder<T> singletonCollector(@NotNull Supplier<T> dataCollector) {
      return this.dataCollector(() -> Collections.singleton(dataCollector.get()));
    }

    public @NotNull DataSyncHandler<T> build() {
      Verify.verifyNotNull(this.key, "no key given");
      Verify.verifyNotNull(this.writer, "no writer given");
      Verify.verifyNotNull(this.converter, "no converter given");
      Verify.verifyNotNull(this.dataCollector, "no data collector given");
      Verify.verifyNotNull(this.nameExtractor, "no name extractor given");
      Verify.verifyNotNull(this.currentGetter, "no current value getter given");

      return new DataSyncHandler<>(
        this.key,
        this.alwaysForceApply,
        this.converter,
        this.writer,
        this.currentGetter,
        this.nameExtractor,
        this.dataCollector);
    }
  }
}
