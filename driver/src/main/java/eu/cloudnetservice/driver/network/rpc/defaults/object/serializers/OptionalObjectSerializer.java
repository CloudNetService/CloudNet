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

package eu.cloudnetservice.driver.network.rpc.defaults.object.serializers;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An object serializer which can read and write an optional to/from a buffer by unwrapping the value and re-wrapping
 * it.
 *
 * @since 4.0
 */
public class OptionalObjectSerializer implements ObjectSerializer<Optional<?>> {

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Optional<?> read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    // check if the optional value was present
    var isPresent = source.startTransaction().readBoolean();
    if (isPresent) {
      // ensure that the given type is parametrized
      Preconditions.checkState(type instanceof ParameterizedType,
        "Optional rpc read called without parameterized type");
      // read the argument type
      var argumentType = ((ParameterizedType) type).getActualTypeArguments()[0];
      // read the value of the buffer at the last index
      // (this can not be null by to suppress the warning we treat it as nullable)
      return Optional.ofNullable(caller.readObject(source.redoTransaction(), argumentType));
    } else {
      // the optional value was not present
      return Optional.empty();
    }
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
