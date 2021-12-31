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

package de.dytanic.cloudnet.driver.network.rpc.defaults.object.serializers;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class UUIDObjectSerializer implements ObjectSerializer<UUID> {

  @Override
  public @Nullable Object read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    return new UUID(source.readLong(), source.readLong());
  }

  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull UUID object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    dataBuf.writeLong(object.getMostSignificantBits());
    dataBuf.writeLong(object.getLeastSignificantBits());
  }
}
