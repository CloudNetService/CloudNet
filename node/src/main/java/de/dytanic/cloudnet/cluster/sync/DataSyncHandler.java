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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataSyncHandler<T> {

  private final String key;
  private final DataConverter<T> converter;

  private final Consumer<T> writer;
  private final UnaryOperator<T> currentGetter;
  private final Supplier<Collection<T>> dataCollector;

  protected DataSyncHandler(
    @NotNull String key,
    @NotNull DataConverter<T> converter,
    @NotNull Consumer<T> writer,
    @NotNull UnaryOperator<T> currentGetter,
    @NotNull Supplier<Collection<T>> dataCollector
  ) {
    this.key = key;
    this.converter = converter;
    this.writer = writer;
    this.currentGetter = currentGetter;
    this.dataCollector = dataCollector;
  }

  public static <T> @NotNull Builder<T> builder() {
    return new Builder<>();
  }

  public @NotNull String getKey() {
    return this.key;
  }

  public @NotNull DataConverter<T> getConverter() {
    return this.converter;
  }

  public void write(@NotNull T data) {
    this.writer.accept(data);
  }

  public @Nullable T getCurrent(@NotNull T toGet) {
    return this.currentGetter.apply(toGet);
  }

  public @NotNull Collection<T> getData() {
    return this.dataCollector.get();
  }

  public interface DataConverter<T> {

    void write(@NotNull DataBuf.Mutable target, @Nullable T data);

    @Nullable T parse(@NotNull DataBuf input) throws Exception;
  }

  public static final class Builder<T> {

    private String key;
    private DataConverter<T> converter;

    private Consumer<T> writer;
    private UnaryOperator<T> currentGetter;
    private Supplier<Collection<T>> dataCollector;

    public @NotNull Builder<T> key(@NotNull String key) {
      this.key = key;
      return this;
    }

    public @NotNull Builder<T> converter(@NotNull DataConverter<T> converter) {
      this.converter = converter;
      return this;
    }

    public @NotNull Builder<T> convertObject(@NotNull Type type) {
      return this.converter(new DataConverter<T>() {
        @Override
        public void write(@NotNull DataBuf.Mutable target, @Nullable T data) {
          target.writeObject(data);
        }

        @Override
        public @Nullable T parse(@NotNull DataBuf input) {
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
      Verify.verifyNotNull(this.currentGetter, "no current value getter given");

      return new DataSyncHandler<>(this.key, this.converter, this.writer, this.currentGetter, this.dataCollector);
    }
  }
}
