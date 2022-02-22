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

package eu.cloudnetservice.modules.bridge;

import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.cloudnet.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import lombok.NonNull;

@RPCValidation
public interface BridgeManagement {

  String BRIDGE_PLAYER_DB_NAME = "cloudnet_cloud_players";

  String BRIDGE_CHANNEL_NAME = "bridge_internal_com_channel";
  String BRIDGE_PLAYER_CHANNEL_NAME = "bridge_internal_player_channel";
  String BRIDGE_PLAYER_EXECUTOR_CHANNEL_NAME = "bridge_internal_player_executor_channel";

  @NonNull BridgeConfiguration configuration();

  void configuration(@NonNull BridgeConfiguration configuration);

  @NonNull PlayerManager playerManager();

  void registerServices(@NonNull ServiceRegistry registry);

  void postInit();
}
