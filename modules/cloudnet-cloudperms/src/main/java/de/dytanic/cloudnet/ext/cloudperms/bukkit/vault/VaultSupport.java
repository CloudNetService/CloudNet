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

package de.dytanic.cloudnet.ext.cloudperms.bukkit.vault;

import de.dytanic.cloudnet.driver.permission.IPermissionManagement;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultSupport {

  public static void enable(JavaPlugin plugin, IPermissionManagement permissionManagement) {
    ServicesManager servicesManager = plugin.getServer().getServicesManager();

    Permission vaultPermissionImplementation = new VaultPermissionImplementation(permissionManagement);

    servicesManager.register(Permission.class, vaultPermissionImplementation, plugin, ServicePriority.Highest);
    servicesManager
      .register(Chat.class, new VaultChatImplementation(vaultPermissionImplementation, permissionManagement), plugin,
        ServicePriority.Highest);
  }

}
