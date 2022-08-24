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

package eu.cloudnetservice.modules.cloudperms.bungee;

import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.driver.util.ModuleUtil;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeCloudPermissionsPlugin extends Plugin {

  @Override
  public void onEnable() {
    this.getProxy().getPluginManager().registerListener(
      this,
      new BungeeCloudPermissionsPlayerListener(CloudNetDriver.instance().permissionManagement()));
  }

  @Override
  public void onDisable() {
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
