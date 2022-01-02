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

package eu.cloudnetservice.plugins.simplenametags.bukkit;

import eu.cloudnetservice.plugins.simplenametags.SimpleNameTagsManager;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitSimpleNameTagsPlugin extends JavaPlugin implements Listener {

  private final SimpleNameTagsManager<Player> nameTagsManager = new BukkitSimpleNameTagsManager(
    runnable -> Bukkit.getScheduler().runTask(this, runnable));

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
  }

  @EventHandler
  public void handle(@NonNull PlayerJoinEvent event) {
    this.nameTagsManager.updateNameTagsFor(event.getPlayer());
  }
}
