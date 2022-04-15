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

import eu.cloudnetservice.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.modules.bridge.config.BridgeConfiguration;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 *
 * @since 4.0
 */
@RPCValidation
public interface BridgeManagement {

  String BRIDGE_PLAYER_DB_NAME = "cloudnet_cloud_players";

  String BRIDGE_CHANNEL_NAME = "bridge_internal_com_channel";
  String BRIDGE_PLAYER_CHANNEL_NAME = "bridge_internal_player_channel";
  String BRIDGE_PLAYER_EXECUTOR_CHANNEL_NAME = "bridge_internal_player_executor_channel";

  /**
   * Gets the bridge configuration that is currently loaded and applied.
   *
   * @return the currently loaded configuration.
   */
  @NonNull BridgeConfiguration configuration();

  /**
   * Sets the bridge configuration for every connected component in the cluster that is running the bridge.
   *
   * @param configuration the configuration to set.
   * @throws NullPointerException if the given configuration is null.
   */
  void configuration(@NonNull BridgeConfiguration configuration);

  /**
   * Gets the player manager provided by this bridge management instance. The player manager is registered in the
   * {@link ServiceRegistry} too.
   *
   * @return the player manager of this bridge management.
   */
  @NonNull PlayerManager playerManager();

  /**
   * Registers all services provided by this bridge management to the service registry.
   *
   * @param registry the registry to register the services in.
   * @throws NullPointerException if the given registry is null.
   */
  void registerServices(@NonNull ServiceRegistry registry);

  /**
   * Execute the post initialization of the bridge management. Populating the caches and applying one time actions.
   * <p>
   * Note: This method should not be used as part of the api and is for internal use only.
   */
  @Internal
  void postInit();
}
