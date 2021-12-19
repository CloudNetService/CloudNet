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

package de.dytanic.cloudnet.driver.network.rpc.defaults.object.serializers;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf.Mutable;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class FunctionalObjectSerializer<T> implements ObjectSerializer<T> {

  private final Function<DataBuf, T> reader;
  private final BiConsumer<DataBuf.Mutable, T> writer;

  protected FunctionalObjectSerializer(Function<DataBuf, T> reader, BiConsumer<Mutable, T> writer) {
    this.reader = reader;
    this.writer = writer;
  }

  public static @NonNull <T> FunctionalObjectSerializer<T> of(
    @NonNull Function<DataBuf, T> reader,
    @NonNull BiConsumer<Mutable, T> writer
  ) {
    return new FunctionalObjectSerializer<>(reader, writer);
  }

  @Override
  public @Nullable T read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    return this.reader.apply(source);
  }

  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @Nullable T object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    this.writer.accept(dataBuf, object);
  }
}
