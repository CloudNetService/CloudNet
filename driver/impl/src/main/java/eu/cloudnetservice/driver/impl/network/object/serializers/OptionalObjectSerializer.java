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
import io.leangen.geantyref.GenericTypeReflector;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import lombok.NonNull;

/**
 * An object serializer which can read and write an optional to/from a buffer by unwrapping the value and re-wrapping
 * it.
 *
 * @since 4.0
 */
public final class OptionalObjectSerializer implements ObjectSerializer<Optional<?>> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Optional<?> read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    if (!(type instanceof ParameterizedType pt) || !GenericTypeReflector.isFullyBound(pt)) {
      // not a parameterized type or bounds are not tight enough to deserialize (e.g. Optional<?>)
      throw new IllegalArgumentException(
        String.format("expected fully bound parameterized type to deserialize Optional, got %s", type));
    }

    var typeArguments = pt.getActualTypeArguments();
    if (typeArguments.length != 1) {
      // must have one type argument to deserialize
      throw new IllegalArgumentException(
        String.format("expected 1 type argument to deserialize Optional, got %d", typeArguments.length));
    }

    // deserialize the value and wrap it in an optional
    var deserializedValue = caller.readObject(source, typeArguments[0]);
    return Optional.ofNullable(deserializedValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Optional<?> object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    caller.writeObject(dataBuf, object.orElse(null));
  }
}
