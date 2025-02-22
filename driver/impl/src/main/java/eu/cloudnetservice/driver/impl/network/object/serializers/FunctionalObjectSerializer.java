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

package eu.cloudnetservice.driver.impl.network.object.serializers;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import eu.cloudnetservice.driver.network.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An object serializer instance which passes the read and write calls to the functional providers previously added.
 *
 * @param <T> the type of data to write/read from the buffer.
 * @since 4.0
 */
public final class FunctionalObjectSerializer<T> implements ObjectSerializer<T> {

  private final Function<DataBuf, T> reader;
  private final BiConsumer<DataBuf.Mutable, T> writer;

  /**
   * Constructs a new functional object serializer instance.
   *
   * @param reader the reader to read the content from the buffer.
   * @param writer the writer which writes the given data to the buffer.
   * @throws NullPointerException if either the writer or reader is null.
   */
  private FunctionalObjectSerializer(
    @NonNull Function<DataBuf, T> reader,
    @NonNull BiConsumer<DataBuf.Mutable, T> writer
  ) {
    this.reader = reader;
    this.writer = writer;
  }

  /**
   * Constructs a new functional object serializer instance.
   *
   * @param reader the reader to read the content from the buffer.
   * @param writer the writer which writes the given data to the buffer.
   * @return the created functional object serializer instance.
   * @throws NullPointerException if either the writer or reader is null.
   */
  public static @NonNull <T> FunctionalObjectSerializer<T> of(
    @NonNull Function<DataBuf, T> reader,
    @NonNull BiConsumer<DataBuf.Mutable, T> writer
  ) {
    return new FunctionalObjectSerializer<>(reader, writer);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable T read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    return this.reader.apply(source);
  }

  /**
   * {@inheritDoc}
   */
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
