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

package eu.cloudnetservice.modules.cloudperms.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.cloudperms.PermissionsUpdateListener;
import eu.cloudnetservice.modules.cloudperms.nukkit.listener.NukkitCloudPermissionsPlayerListener;

public final class NukkitCloudPermissionsPlugin extends PluginBase {

  @Override
  public void onEnable() {
    super.getServer().getPluginManager().registerEvents(
      new NukkitCloudPermissionsPlayerListener(CloudNetDriver.instance().permissionManagement()),
      this);
    CloudNetDriver.instance().eventManager().registerListener(new PermissionsUpdateListener<>(
      runnable -> Server.getInstance().getScheduler().scheduleTask(this, runnable),
      Player::sendCommandData,
      Player::getUniqueId,
      uuid -> Server.getInstance().getPlayer(uuid).orElse(null),
      () -> Server.getInstance().getOnlinePlayers().values()));
  }

  @Override
  public void onDisable() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
