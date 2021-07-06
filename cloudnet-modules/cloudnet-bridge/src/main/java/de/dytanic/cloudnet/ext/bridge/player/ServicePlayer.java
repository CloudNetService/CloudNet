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

package de.dytanic.cloudnet.ext.bridge.player;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.ext.bridge.bukkit.BukkitCloudNetPlayerInfo;
import de.dytanic.cloudnet.ext.bridge.bungee.BungeeCloudNetPlayerInfo;
import de.dytanic.cloudnet.ext.bridge.nukkit.NukkitCloudNetPlayerInfo;
import de.dytanic.cloudnet.ext.bridge.velocity.VelocityCloudNetPlayerInfo;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class ServicePlayer {

  private final JsonDocument data;

  public ServicePlayer(@NotNull JsonDocument data) {
    this.data = data;
  }

  @NotNull
  public JsonDocument getRawData() {
    return this.data;
  }

  @NotNull
  public UUID getUniqueId() {
    return this.data.get("uniqueId", UUID.class);
  }

  @NotNull
  public String getName() {
    return this.data.getString("name");
  }

  @NotNull
  public BukkitCloudNetPlayerInfo asBukkit() {
    return this.data.toInstanceOf(BukkitCloudNetPlayerInfo.class);
  }

  @NotNull
  public NukkitCloudNetPlayerInfo asNukkit() {
    return this.data.toInstanceOf(NukkitCloudNetPlayerInfo.class);
  }

  @NotNull
  public BungeeCloudNetPlayerInfo asBungee() {
    return this.data.toInstanceOf(BungeeCloudNetPlayerInfo.class);
  }

  @NotNull
  public VelocityCloudNetPlayerInfo asVelocity() {
    return this.data.toInstanceOf(VelocityCloudNetPlayerInfo.class);
  }

}
