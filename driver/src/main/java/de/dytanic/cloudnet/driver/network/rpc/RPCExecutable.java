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

package de.dytanic.cloudnet.driver.network.rpc;

import de.dytanic.cloudnet.common.concurrent.Task;
import de.dytanic.cloudnet.driver.network.NetworkChannel;
import lombok.NonNull;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;

public interface RPCExecutable {

  @NonBlocking
  void fireAndForget();

  @Blocking
  <T> T fireSync();

  @NonNull <T> Task<T> fire();

  @NonBlocking
  void fireAndForget(@NonNull NetworkChannel component);

  @Blocking
  <T> T fireSync(@NonNull NetworkChannel component);

  @NonNull <T> Task<T> fire(@NonNull NetworkChannel component);
}
