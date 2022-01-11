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

package eu.cloudnetservice.cloudnet.driver.network.rpc.object;

import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface ObjectSerializer<T> {

  @Nullable Object read(@NonNull DataBuf source, @NonNull Type type, @NonNull ObjectMapper caller);

  void write(@NonNull DataBuf.Mutable dataBuf, @NonNull T object, @NonNull Type type, @NonNull ObjectMapper caller);

  default boolean preWriteCheckAccepts(@NonNull Object object) {
    return true;
  }

  default boolean preReadCheckAccepts(@NonNull Type type) {
    return true;
  }
}
