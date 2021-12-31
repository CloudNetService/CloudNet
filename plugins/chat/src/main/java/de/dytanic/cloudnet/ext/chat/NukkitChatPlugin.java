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

package de.dytanic.cloudnet.ext.chat;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import lombok.NonNull;

public class NukkitChatPlugin extends PluginBase implements Listener {

  private String format;

  @Override
  public void onEnable() {
    super.saveDefaultConfig();
    this.format = super.getConfig().getString("format", "%display%%name% &8:&f %message%");
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void handle(@NonNull PlayerChatEvent event) {
    var player = event.getPlayer();
    var format = ChatFormatter.buildFormat(
      player.getUniqueId(),
      player.getName(),
      player.getDisplayName(),
      this.format,
      event.getMessage(),
      event.getPlayer()::hasPermission,
      TextFormat::colorize
    );
    if (format == null) {
      event.setCancelled(true);
    } else {
      event.setFormat(format);
    }
  }
}
