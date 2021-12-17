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

package de.dytanic.cloudnet.ext.bridge.node.event;

import de.dytanic.cloudnet.driver.event.events.DriverEvent;
import de.dytanic.cloudnet.ext.bridge.player.NetworkPlayerProxyInfo;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class LocalPlayerPreLoginEvent extends DriverEvent {

  private final NetworkPlayerProxyInfo playerInfo;
  private Result result = Result.allowed();

  public LocalPlayerPreLoginEvent(@NotNull NetworkPlayerProxyInfo playerInfo) {
    this.playerInfo = playerInfo;
  }

  public @NotNull NetworkPlayerProxyInfo playerInfo() {
    return this.playerInfo;
  }

  public @NotNull Result result() {
    return this.result;
  }

  public void result(@NotNull Result result) {
    this.result = result;
  }

  public static final class Result {

    private static final Result ALLOWED = new Result(true, null);

    private final boolean allowed;
    private final TextComponent result;

    private Result(boolean allowed, @Nullable TextComponent result) {
      this.allowed = allowed;
      this.result = result;
    }

    public static @NotNull Result allowed() {
      return ALLOWED;
    }

    public static @NotNull Result denied(@Nullable TextComponent reason) {
      return new Result(false, reason);
    }

    public boolean isAllowed() {
      return this.allowed;
    }

    public @UnknownNullability TextComponent result() {
      return this.result;
    }
  }
}
