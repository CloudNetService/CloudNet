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

package eu.cloudnetservice.modules.bridge.player.executor;

import eu.cloudnetservice.driver.network.rpc.annotation.RPCValidation;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;

@RPCValidation
public interface PlayerExecutor {

  UUID GLOBAL_UNIQUE_ID = new UUID(0, 0);

  /**
   * Gets the uniqueId of the player this player executor handles.
   *
   * @return the UUID of the player
   */
  @NonNull UUID uniqueId();

  /**
   * Connects an online player to a specific service.
   *
   * @param serviceName the name of the service the player should be sent to
   */
  void connect(@NonNull String serviceName);

  /**
   * Connects an online player to one service selected by the selector out of the global list of services.
   *
   * @param selectorType the type for sorting
   */
  void connectSelecting(@NonNull ServerSelectorType selectorType);

  /**
   * Connects an online player to a fallback exactly like it is done in the hub command or on login.
   */
  void connectToFallback();

  /**
   * Connects an online player to one service selected by the selector out of the global list of services with the given
   * group.
   *
   * @param selectorType the type for sorting
   */
  void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType);

  /**
   * Connects an online player to one service selected by the selector out of the global list of services with the given
   * task.
   *
   * @param selectorType the type for sorting
   */
  void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType);

  /**
   * Kicks an online player from the network with a specific reason.
   *
   * @param message the reason for the kick which will be displayed in the players client
   */
  void kick(@NonNull Component message);

  void sendTitle(@NonNull Title title);

  /**
   * Sends a plugin message to a specific online player.
   *
   * @param message the message to be sent to the player
   */
  void sendMessage(@NonNull Component message);

  /**
   * Sends a plugin message to a specific online player only if the player has the given permission. If the permission
   * is null, the message will be sent.
   *
   * @param message    the message to be sent to the player
   * @param permission the permission which will be checked before the message is sent to the player
   */
  void sendChatMessage(@NonNull Component message, @Nullable String permission);

  /**
   * Sends a message to a specific online player. The tag has to be registered in the proxy.
   *
   * @param tag  the tag of the plugin message
   * @param data the data of the plugin message
   */
  void sendPluginMessage(@NonNull String tag, byte[] data);

  /**
   * Dispatches a command on the proxy the player is connected with as the player.
   *
   * @param command the command to dispatch
   */
  void spoofCommandExecution(@NonNull String command);
}
