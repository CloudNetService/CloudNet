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

package de.dytanic.cloudnet.driver.network.rpc.object;

import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ObjectSerializer<T> {

  @Nullable Object read(@NotNull DataBuf source, @NotNull Type type, @NotNull ObjectMapper caller);

  @SuppressWarnings("unchecked")
  default void writeObject(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull Object object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    this.write(dataBuf, (T) object, type, caller);
  }

  default void write(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull T object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    throw new UnsupportedOperationException();
  }
}
