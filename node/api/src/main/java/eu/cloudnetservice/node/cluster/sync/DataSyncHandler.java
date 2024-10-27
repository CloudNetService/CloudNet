/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.cluster.sync;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface DataSyncHandler<T> {

  static <T> DataSyncHandler.@NonNull Builder<T> builder() {
    //noinspection unchecked
    return ServiceRegistry.registry().defaultInstance(DataSyncHandler.Builder.class);
  }

  @NonNull
  String name(@NonNull Object obj);

  void write(@NonNull Object data);

  void serialize(@NonNull DataBuf.Mutable target, @NonNull Object data);

  @Nullable
  T current(@NonNull Object toGet);

  @NonNull
  Collection<T> data();

  @NonNull
  String key();

  boolean alwaysForceApply();

  @NonNull
  DataConverter<T> converter();

  @NonNull
  Consumer<T> writer();

  @NonNull
  UnaryOperator<T> currentGetter();

  @NonNull
  Function<T, String> nameExtractor();

  @NonNull
  Supplier<Collection<T>> dataCollector();

  interface DataConverter<T2> {

    void write(@NonNull DataBuf.Mutable target, @NonNull T2 data);

    @NonNull
    T2 parse(@NonNull DataBuf input) throws Exception;
  }

  interface Builder<T> {

    @NonNull
    Builder<T> key(@NonNull String key);

    @NonNull
    Builder<T> alwaysForce();

    @NonNull
    Builder<T> converter(@NonNull DataSyncHandler.DataConverter<T> converter);

    @NonNull
    Builder<T> convertObject(@NonNull Type type);

    @NonNull
    Builder<T> writer(@NonNull Consumer<T> writer);

    @NonNull
    Builder<T> currentGetter(@NonNull UnaryOperator<T> currentGetter);

    @NonNull
    Builder<T> nameExtractor(@NonNull Function<T, String> nameExtractor);

    @NonNull
    Builder<T> dataCollector(@NonNull Supplier<Collection<T>> dataCollector);

    @NonNull
    Builder<T> singletonCollector(@NonNull Supplier<T> dataCollector);

    @NonNull
    DataSyncHandler<T> build();
  }
}
