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

package eu.cloudnetservice.modules.bridge.platform;

import eu.cloudnetservice.cloudnet.driver.network.rpc.RPC;
import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import java.util.UUID;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;

final class PlatformPlayerExecutor implements PlayerExecutor {

  private final RPC baseRPC;
  private final RPCSender sender;
  private final UUID targetUniqueId;

  public PlatformPlayerExecutor(@NonNull RPC baseRPC, @NonNull UUID targetUniqueId) {
    this.baseRPC = baseRPC;
    this.targetUniqueId = targetUniqueId;
    this.sender = baseRPC.sender().factory().providerForClass(null, PlayerExecutor.class);
  }

  @Override
  public @NonNull UUID uniqueId() {
    return this.targetUniqueId;
  }

  @Override
  public void connect(@NonNull String serviceName) {
    this.baseRPC.join(this.sender.invokeMethod("connect", serviceName)).fireSync();
  }

  @Override
  public void connectSelecting(@NonNull ServerSelectorType selectorType) {
    this.baseRPC.join(this.sender.invokeMethod("connectSelecting", selectorType)).fireSync();
  }

  @Override
  public void connectToFallback() {
    this.baseRPC.join(this.sender.invokeMethod("connectToFallback")).fireSync();
  }

  @Override
  public void connectToGroup(@NonNull String group, @NonNull ServerSelectorType selectorType) {
    this.baseRPC.join(this.sender.invokeMethod("connectToGroup", group, selectorType)).fireSync();
  }

  @Override
  public void connectToTask(@NonNull String task, @NonNull ServerSelectorType selectorType) {
    this.baseRPC.join(this.sender.invokeMethod("connectToTask", task, selectorType)).fireSync();
  }

  @Override
  public void kick(@NonNull Component message) {
    this.baseRPC.join(this.sender.invokeMethod("kick", message)).fireSync();
  }

  @Override
  public void sendTitle(@NonNull Title title) {
    this.baseRPC.join(this.sender.invokeMethod("sendTitle", title)).fireSync();
  }

  @Override
  public void sendMessage(@NonNull Component message) {
    this.baseRPC.join(this.sender.invokeMethod("sendMessage", message)).fireSync();
  }

  @Override
  public void sendChatMessage(@NonNull Component message, @Nullable String permission) {
    this.baseRPC.join(this.sender.invokeMethod("sendChatMessage", message, permission)).fireSync();
  }

  @Override
  public void sendPluginMessage(@NonNull String tag, byte[] data) {
    this.baseRPC.join(this.sender.invokeMethod("sendPluginMessage", tag, data)).fireSync();
  }

  @Override
  public void spoofCommandExecution(@NonNull String command) {
    this.baseRPC.join(this.sender.invokeMethod("spoofCommandExecution", command)).fireSync();
  }
}
