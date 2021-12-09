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

package de.dytanic.cloudnet.ext.bridge.platform;

import de.dytanic.cloudnet.driver.network.rpc.RPC;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.ext.bridge.player.executor.PlayerExecutor;
import de.dytanic.cloudnet.ext.bridge.player.executor.ServerSelectorType;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PlatformPlayerExecutor implements PlayerExecutor {

  private final RPC baseRPC;
  private final RPCSender sender;
  private final UUID targetUniqueId;

  public PlatformPlayerExecutor(@NotNull RPC baseRPC, @NotNull UUID targetUniqueId) {
    this.baseRPC = baseRPC;
    this.targetUniqueId = targetUniqueId;
    this.sender = baseRPC.getSender().getFactory().providerForClass(null, PlayerExecutor.class);
  }

  @Override
  public @NotNull UUID getPlayerUniqueId() {
    return this.targetUniqueId;
  }

  @Override
  public void connect(@NotNull String serviceName) {
    this.baseRPC.join(this.sender.invokeMethod("connect", serviceName)).fireSync();
  }

  @Override
  public void connectSelecting(@NotNull ServerSelectorType selectorType) {
    this.baseRPC.join(this.sender.invokeMethod("connect", selectorType)).fireSync();
  }

  @Override
  public void connectToFallback() {
    this.baseRPC.join(this.sender.invokeMethod("connectToFallback")).fireSync();
  }

  @Override
  public void connectToGroup(@NotNull String group, @NotNull ServerSelectorType selectorType) {
    this.baseRPC.join(this.sender.invokeMethod("connectToGroup", group, selectorType)).fireSync();
  }

  @Override
  public void connectToTask(@NotNull String task, @NotNull ServerSelectorType selectorType) {
    this.baseRPC.join(this.sender.invokeMethod("connectToTask", task, selectorType)).fireSync();
  }

  @Override
  public void kick(@NotNull Component message) {
    this.baseRPC.join(this.sender.invokeMethod("kick", message)).fireSync();
  }

  @Override
  public void sendTitle(@NotNull Title title) {
    this.baseRPC.join(this.sender.invokeMethod("sendTitle", title)).fireSync();
  }

  @Override
  public void sendMessage(@NotNull Component message) {
    this.baseRPC.join(this.sender.invokeMethod("sendChatMessage", message)).fireSync();
  }

  @Override
  public void sendChatMessage(@NotNull Component message, @Nullable String permission) {
    this.baseRPC.join(this.sender.invokeMethod("sendChatMessage", message, permission)).fireSync();
  }

  @Override
  public void sendPluginMessage(@NotNull String tag, byte[] data) {
    this.baseRPC.join(this.sender.invokeMethod("sendPluginMessage", tag, data)).fireSync();
  }

  @Override
  public void dispatchProxyCommand(@NotNull String command) {
    this.baseRPC.join(this.sender.invokeMethod("dispatchProxyCommand", command)).fireSync();
  }
}
