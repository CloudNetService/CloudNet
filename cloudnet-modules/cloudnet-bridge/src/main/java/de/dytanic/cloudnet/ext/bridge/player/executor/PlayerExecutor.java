package de.dytanic.cloudnet.ext.bridge.player.executor;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlayerExecutor {

  /**
   * Gets the uniqueId of the player this player executor handles.
   *
   * @return the UUID of the player
   */
  @NotNull
  UUID getPlayerUniqueId();

  /**
   * Connects an online player to a specific service.
   *
   * @param serviceName the name of the service the player should be sent to
   */
  void connect(@NotNull String serviceName);

  /**
   * Connects an online player to one service selected by the selector out of the global list of services.
   *
   * @param selectorType the type for sorting
   */
  void connect(@NotNull ServerSelectorType selectorType);

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
  void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType);

  /**
   * Connects an online player to one service selected by the selector out of the global list of services with the given
   * task.
   *
   * @param selectorType the type for sorting
   */
  void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType);

  /**
   * Kicks an online player from the network with a specific reason.
   *
   * @param message the reason for the kick which will be displayed in the players client
   */
  void kick(@NotNull String message);

  /**
   * Sends a plugin message to a specific online player.
   *
   * @param message the message to be sent to the player
   */
  void sendChatMessage(@NotNull String message);

  /**
   * Sends a plugin message to a specific online player only if the player has the given permission. If the permission
   * is null, the message will be sent.
   *
   * @param message    the message to be sent to the player
   * @param permission the permission which will be checked before the message is sent to the player
   */
  void sendChatMessage(@NotNull String message, @Nullable String permission);

  /**
   * Sends a message to a specific online player. The tag has to be registered in the proxy.
   *
   * @param tag  the tag of the plugin message
   * @param data the data of the plugin message
   */
  void sendPluginMessage(@NotNull String tag, @NotNull byte[] data);

  /**
   * Dispatches a command on the proxy the player is connected with as the player.
   *
   * @param command the command to dispatch
   */
  void dispatchProxyCommand(@NotNull String command);

}
