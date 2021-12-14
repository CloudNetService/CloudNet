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
import java.util.logging.Level;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

public final class VaultSupport {

  private VaultSupport() {
    throw new UnsupportedOperationException();
  }

  public static void hook(@NotNull Plugin plugin, @NotNull IPermissionManagement management) {
    try {
      var services = plugin.getServer().getServicesManager();

      Permission vaultPermissions = new VaultPermissionImplementation(management);
      Chat vaultChat = new VaultChatImplementation(vaultPermissions, management);

      services.register(Permission.class, vaultPermissions, plugin, ServicePriority.High);
      services.register(Chat.class, vaultChat, plugin, ServicePriority.High);
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Exception occurred while hooking into vault", exception);
    }
  }
}
