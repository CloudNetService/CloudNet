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

package de.dytanic.cloudnet.ext.bridge;

import de.dytanic.cloudnet.common.registry.IServicesRegistry;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCValidation;
import de.dytanic.cloudnet.ext.bridge.config.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.player.IPlayerManager;
import org.jetbrains.annotations.NotNull;

@RPCValidation
public interface BridgeManagement {

  String BRIDGE_PLAYER_DB_NAME = "cloudnet_cloud_players";

  String BRIDGE_CHANNEL_NAME = "bridge_internal_com_channel";
  String BRIDGE_PLAYER_CHANNEL_NAME = "bridge_internal_player_channel";
  String BRIDGE_PLAYER_EXECUTOR_CHANNEL_NAME = "bridge_internal_player_executor_channel";

  @NotNull BridgeConfiguration getConfiguration();

  void setConfiguration(@NotNull BridgeConfiguration configuration);

  @NotNull IPlayerManager getPlayerManager();

  void registerServices(@NotNull IServicesRegistry registry);

  void postInit();
}
