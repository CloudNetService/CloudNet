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

package eu.cloudnetservice.modules.bridge.player;

import eu.cloudnetservice.driver.network.HostAndPort;
import java.util.UUID;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A network player server info represents the downstream service a cloud player is connected to.
 *
 * @param uniqueId       the unique id of the player.
 * @param name           the name of the player.
 * @param xBoxId         the xbox id of the player, null if the player does not have a xbox id.
 * @param address        the host address of the player.
 * @param networkService the service info of the downstream service the player is connected to.
 * @see NetworkServiceInfo
 * @since 4.0
 */
public record NetworkPlayerServerInfo(
  @NonNull UUID uniqueId,
  @NonNull String name,
  @Nullable String xBoxId,
  @NonNull HostAndPort address,
  @NonNull NetworkServiceInfo networkService
) {

}
