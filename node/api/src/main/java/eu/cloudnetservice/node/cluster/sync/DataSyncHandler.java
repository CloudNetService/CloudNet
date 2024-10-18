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
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface DataSyncHandler<T> {

  static <T> @NonNull DataSyncHandler.Builder<T> builder() {
    return new DataSyncHandler.Builder<>();
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

    @NonNull T2 parse(@NonNull DataBuf input) throws Exception;
  }
}
