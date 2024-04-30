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

package eu.cloudnetservice.modules.bridge.platform;

import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import java.util.UUID;
import lombok.NonNull;

abstract class PlatformPlayerExecutor implements PlayerExecutor {

  private final UUID targetUniqueId;

  public PlatformPlayerExecutor(@NonNull UUID targetUniqueId) {
    this.targetUniqueId = targetUniqueId;
  }

  @Override
  public @NonNull UUID uniqueId() {
    return this.targetUniqueId;
  }
}
