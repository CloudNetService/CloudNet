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

public interface ObjectMapper {

  @NotNull ObjectMapper unregisterBinding(@NotNull Type type, boolean superTypes);

  @NotNull <T> ObjectMapper registerBinding(@NotNull Type type, @NotNull ObjectSerializer<T> serializer,
    boolean superTypes);

  @NotNull DataBuf.Mutable writeObject(@NotNull DataBuf.Mutable dataBuf, @Nullable Object object);

  @Nullable <T> T readObject(@NotNull DataBuf dataBuf, @NotNull Type type);
}
