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

package de.dytanic.cloudnet.ext.cloudperms.gomint;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.cloudperms.gomint.listener.GoMintCloudNetCloudPermissionsPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import io.gomint.GoMint;
import io.gomint.entity.EntityPlayer;
import io.gomint.plugin.Plugin;
import io.gomint.plugin.PluginName;
import io.gomint.plugin.Version;

@PluginName("CloudNet-CloudPerms")
@Version(major = 1, minor = 2)
public final class GoMintCloudNetCloudPermissionsPlugin extends Plugin {

  private static GoMintCloudNetCloudPermissionsPlugin instance;

  public static GoMintCloudNetCloudPermissionsPlugin getInstance() {
    return GoMintCloudNetCloudPermissionsPlugin.instance;
  }

  @Override
  public void onStartup() {
    instance = this;
  }

  @Override
  public void onInstall() {
    this.injectEntityPlayersCloudPermissionManager();

    super.registerListener(
      new GoMintCloudNetCloudPermissionsPlayerListener(CloudNetDriver.getInstance().getPermissionManagement()));
  }

  @Override
  public void onUninstall() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());
  }

  private void injectEntityPlayersCloudPermissionManager() {
    GoMint.instance().onlinePlayers().forEach(this::injectPermissionManager);
  }

  public void injectPermissionManager(EntityPlayer entityPlayer) {
    entityPlayer.permissionManager(new GoMintCloudNetCloudPermissionsPermissionManager(entityPlayer,
      CloudNetDriver.getInstance().getPermissionManagement()));
  }

}
