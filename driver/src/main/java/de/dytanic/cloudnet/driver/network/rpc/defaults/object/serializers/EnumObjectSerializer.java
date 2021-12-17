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
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class EnumObjectSerializer implements ObjectSerializer<Enum<?>> {

  private final Map<Type, Object[]> enumConstantCache = new ConcurrentHashMap<>();

  @Override
  public @Nullable Enum<?> read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    // ensure that the method was called using a class as the type
    Verify.verify(type instanceof Class<?>, "Called enum read method without proving a class as type");
    // get the cached enum constants of the class
    var enumConstants = this.enumConstantCache.computeIfAbsent(type, $ -> ((Class<?>) type).getEnumConstants());
    // get the constant associated with the ordinal index
    var ordinal = source.readInt();
    return ordinal >= enumConstants.length ? null : (Enum<?>) enumConstants[ordinal];
  }

  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Enum<?> object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeInt(object.ordinal());
  }
}
