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

package eu.cloudnetservice.modules.bridge.node.event;

import eu.cloudnetservice.driver.event.Event;
import eu.cloudnetservice.modules.bridge.player.NetworkPlayerProxyInfo;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Called before the login of a player is processed and therefore allows determining the outcome of the login process.
 * <p>
 * Note: This event is <strong>ONLY</strong> called on the node the proxy is running on.
 *
 * @since 4.0
 */
public final class LocalPlayerPreLoginEvent extends Event {

  private final NetworkPlayerProxyInfo playerInfo;
  private Result result = Result.allowed();

  /**
   * Constructs a new player pre login event with the given player proxy info.
   *
   * @param playerInfo the player info of the player connecting.
   * @throws NullPointerException if the player proxy info is null.
   */
  public LocalPlayerPreLoginEvent(@NonNull NetworkPlayerProxyInfo playerInfo) {
    this.playerInfo = playerInfo;
  }

  /**
   * Gets the player proxy info for the player requesting a login.
   *
   * @return the player proxy info.
   */
  public @NonNull NetworkPlayerProxyInfo playerInfo() {
    return this.playerInfo;
  }

  /**
   * Gets the result of the login process. If {@link Result#permitLogin()} is true the player is allowed to log in and
   * denied otherwise.
   *
   * @return the result of the login process.
   */
  public @NonNull Result result() {
    return this.result;
  }

  /**
   * Sets the result of the login process. To deny a login use {@link Result#denied(Component)} and
   * {@link Result#allowed()} to allow the login.
   *
   * @param result the result to set for the login.
   * @throws NullPointerException if the result is null.
   */
  public void result(@NonNull Result result) {
    this.result = result;
  }

  /**
   * Represents the outcome of the login event.
   *
   * @since 4.0
   */
  public static final class Result {

    private static final Result ALLOWED = new Result(true, null);

    private final boolean allowed;
    private final Component result;

    /**
     * Constructs a new result for the player pre login event.
     *
     * @param allowed whether the player is allowed to connect or not.
     * @param result  the reason for denying the login.
     */
    private Result(boolean allowed, @Nullable Component result) {
      this.allowed = allowed;
      this.result = result;
    }

    /**
     * Gets a jvm static result that allows the login.
     *
     * @return an allowing result.
     */
    public static @NonNull Result allowed() {
      return ALLOWED;
    }

    /**
     * Creates a new result that denies the login of the player and sets the given reason.
     *
     * @param reason the reason for denying the login.
     * @return the new result denying the login.
     */
    public static @NonNull Result denied(@Nullable Component reason) {
      return new Result(false, reason);
    }

    /**
     * Gets whether this result allows the login or not.
     *
     * @return true if the login is allowed, false otherwise.
     */
    public boolean permitLogin() {
      return this.allowed;
    }

    /**
     * Gets the text component that is displayed to the player if the login is denied.
     *
     * @return the reason to display to the player, might be null if the component is not set.
     */
    public @UnknownNullability Component result() {
      return this.result;
    }
  }
}
