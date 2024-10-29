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

package eu.cloudnetservice.modules.bridge.player.executor;

import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;

/**
 * The player executor is used to perform various platform dependent actions. The player executor must be distinguished
 * in two ways:
 * <ul>
 *   <li> A global player executor performs the actions for all players connected to the network.
 *   It can be recognized by the fact that it uses the {@link #GLOBAL_UNIQUE_ID} as its unique id.
 *   The global player executor can be obtained with {@link PlayerManager#globalPlayerExecutor()}.
 *   </li>
 *   <li>A player executor, who executes the actions for a specific player and only as long as the player is online.
 *   The per player executor can be obtained with {@link PlayerManager#playerExecutor(UUID)}</li>
 * </ul>
 *
 * @since 4.0
 */
public interface PlayerExecutor {

  UUID GLOBAL_UNIQUE_ID = new UUID(0, 0);

  /**
   * Gets the unique id of this player executor. The ID of the player executor equals the unique id of the player this
   * executor is for.
   * <p>
   * A global player executor that performs the action for all players is identified by the {@link #GLOBAL_UNIQUE_ID}.
   *
   * @return the unique id of the player executor.
   */
  @NonNull
  UUID uniqueId();

  /**
   * Connects the player associated with this player executor to the given service.
   *
   * @param serviceName the service to connect the player to.
   * @throws NullPointerException if the given service name is null.
   */
  void connect(@NonNull String serviceName);

  /**
   * Connects the player associated with this player executor to any service running the cluster. The service is chosen
   * by the given server selector.
   *
   * @param selectorType the selector used for the service selection.
   * @throws NullPointerException if the selector type is null.
   */
  void connectSelecting(@NonNull ServerSelectorType selectorType);

  /**
   * Connects the player associated with this player executor to the best matching fallback.
   */
  void connectToFallback();

  /**
   * Connects the player associated with this player executor to any service having the given group assigned. The
   * service is chosen by the given server selector.
   *
   * @param group        the group a service has to have.
   * @param selectorType the selector used for the service selection.
   * @throws NullPointerException if the given group or selector is null.
   */
  void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType);

  /**
   * Connects the player associated with this player executor to any service having the given task assigned. The service
   * is chosen by the given server selector.
   *
   * @param task         the task the service has to have.
   * @param selectorType the selector used for the service selection.
   * @throws NullPointerException if the given group or selector is null.
   */
  void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType);

  /**
   * Kicks the player associated with this player executor and the component as reason.
   *
   * @param message the message to display in the disconnect screen.
   * @throws NullPointerException if the given message is null.
   */
  void kick(@NonNull Component message);

  /**
   * Sends the given title to the player associated with this player executor.
   *
   * @param title the title to display to the player.
   * @throws NullPointerException if the given title is null.
   */
  void sendTitle(@NonNull Title title);

  /**
   * Sends the given message component as chat message to the player associated with this player executor.
   * <p>
   * This method is equivalent to calling {@code playerExecutor.sendChatMessage(message, null)}.
   *
   * @param message the message to send to the player.
   * @throws NullPointerException if the given component is null.
   */
  default void sendChatMessage(@NonNull Component message) {
    this.sendChatMessage(message, null);
  }

  /**
   * Sends the given message component as chat message to the player associated with this player executor.
   * <p>
   * If a permission is given the player is required to have the permission, otherwise the message is not sent.
   *
   * @param message    the message to send to the player.
   * @param permission the permission required to get the message.
   * @throws NullPointerException if the given component is null.
   */
  void sendChatMessage(@NonNull Component message, @Nullable String permission);

  /**
   * Sends a plugin channel message to the player associated with this player executor using the given channel and data.
   * The channel used has to be registered on the service software.
   *
   * @param key  the channel to send the plugin message in.
   * @param data the data to send to the player.
   * @throws NullPointerException if the given channel or data is null.
   */
  void sendPluginMessage(@NonNull String key, byte[] data);

  /**
   * Spoofs the command execution for the player associated with this player executor. The given command input should
   * not contain a leading slash as it is added on the fly.
   * <p>
   * The command is executed on the proxy and if no command was found the command is redirected to the downstream
   * service the player is connected to.
   * <p>
   * Note: This method is equivalent to calling {@code playerExecutor.spoofCommandExecution(command, true)}
   *
   * @param command the command to execute for the player.
   * @throws NullPointerException if the given command is null.
   */
  default void spoofCommandExecution(@NonNull String command) {
    this.spoofCommandExecution(command, true);
  }

  /**
   * Spoofs the command execution for the player associated with this player executor. The given command input should
   * not contain a leading slash as it is added on the fly.
   * <p>
   * If redirecting to the downstream service is desired the command is executed on the proxy first, if no command was
   * found it is redirected to the downstream service the player is connected to.
   *
   * @param command          the command to execute for the player.
   * @param redirectToServer whether to command should get redirected to the downstream.
   * @throws NullPointerException if the given command is null.
   */
  void spoofCommandExecution(@NonNull String command, boolean redirectToServer);
}
