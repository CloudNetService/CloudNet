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

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptionalObjectSerializer implements ObjectSerializer<Optional<?>> {

  @Override
  public @Nullable Optional<?> read(
    @NotNull DataBuf source,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // check if the optional value was present
    boolean isPresent = source.startTransaction().readBoolean();
    if (isPresent) {
      // ensure that the given type is parametrized
      Verify.verify(type instanceof ParameterizedType, "Optional rpc read called without parameterized type");
      // read the argument type
      Type argumentType = ((ParameterizedType) type).getActualTypeArguments()[0];
      // read the value of the buffer at the last index
      // (this can not be null by to suppress the warning we treat it as nullable)
      return Optional.ofNullable(caller.readObject(source.redoTransaction(), argumentType));
    } else {
      // the optional value was not present
      return Optional.empty();
    }
  }

  @Override
  public void write(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull Optional<?> object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    caller.writeObject(dataBuf, object.orElse(null));
  }
}
