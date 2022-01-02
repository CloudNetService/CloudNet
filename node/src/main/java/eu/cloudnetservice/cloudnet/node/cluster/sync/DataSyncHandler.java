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

package eu.cloudnetservice.cloudnet.node.cluster.sync;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record DataSyncHandler<T>(
  @NonNull String key,
  boolean alwaysForceApply,
  @NonNull DataConverter<T> converter,
  @NonNull Consumer<T> writer,
  @NonNull UnaryOperator<T> currentGetter,
  @NonNull Function<T, String> nameExtractor,
  @NonNull Supplier<Collection<T>> dataCollector
) {

  public static <T> @NonNull Builder<T> builder() {
    return new Builder<>();
  }

  @SuppressWarnings("unchecked")
  public @NonNull String name(@NonNull Object obj) {
    return this.nameExtractor.apply((T) obj);
  }

  @SuppressWarnings("unchecked")
  public void write(@NonNull Object data) {
    this.writer.accept((T) data);
  }

  @SuppressWarnings("unchecked")
  public void serialize(@NonNull DataBuf.Mutable target, @NonNull Object data) {
    this.converter.write(target, (T) data);
  }

  @SuppressWarnings("unchecked")
  public @Nullable T current(@NonNull Object toGet) {
    return this.currentGetter.apply((T) toGet);
  }

  public @NonNull Collection<T> data() {
    return this.dataCollector.get();
  }

  public interface DataConverter<T> {

    void write(@NonNull DataBuf.Mutable target, @NonNull T data);

    @NonNull T parse(@NonNull DataBuf input) throws Exception;
  }

  public static final class Builder<T> {

    private String key;
    private boolean alwaysForceApply;
    private DataConverter<T> converter;

    private Consumer<T> writer;
    private UnaryOperator<T> currentGetter;
    private Function<T, String> nameExtractor;
    private Supplier<Collection<T>> dataCollector;

    public @NonNull Builder<T> key(@NonNull String key) {
      this.key = key;
      return this;
    }

    public @NonNull Builder<T> alwaysForce() {
      this.alwaysForceApply = true;
      return this;
    }

    public @NonNull Builder<T> converter(@NonNull DataConverter<T> converter) {
      this.converter = converter;
      return this;
    }

    public @NonNull Builder<T> convertObject(@NonNull Type type) {
      return this.converter(new DataConverter<>() {
        @Override
        public void write(@NonNull DataBuf.Mutable target, @NonNull T data) {
          target.writeObject(data);
        }

        @Override
        public @NonNull T parse(@NonNull DataBuf input) {
          return input.readObject(type);
        }
      });
    }

    public @NonNull Builder<T> writer(@NonNull Consumer<T> writer) {
      this.writer = writer;
      return this;
    }

    public @NonNull Builder<T> currentGetter(@NonNull UnaryOperator<T> currentGetter) {
      this.currentGetter = currentGetter;
      return this;
    }

    public @NonNull Builder<T> nameExtractor(@NonNull Function<T, String> nameExtractor) {
      this.nameExtractor = nameExtractor;
      return this;
    }

    public @NonNull Builder<T> dataCollector(@NonNull Supplier<Collection<T>> dataCollector) {
      this.dataCollector = dataCollector;
      return this;
    }

    public @NonNull Builder<T> singletonCollector(@NonNull Supplier<T> dataCollector) {
      return this.dataCollector(() -> List.of(dataCollector.get()));
    }

    public @NonNull DataSyncHandler<T> build() {
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
