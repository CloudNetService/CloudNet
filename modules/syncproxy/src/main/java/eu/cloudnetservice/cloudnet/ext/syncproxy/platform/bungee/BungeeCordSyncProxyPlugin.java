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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.bungee;

import de.dytanic.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.ext.syncproxy.platform.listener.SyncProxyCloudListener;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeCordSyncProxyPlugin extends Plugin {

  private BungeeCordSyncProxyManagement syncProxyManagement;

  @Override
  public void onEnable() {
    this.syncProxyManagement = new BungeeCordSyncProxyManagement(this);
    // register the SyncProxyManagement in our service registry
    this.syncProxyManagement.registerService(Wrapper.getInstance().getServicesRegistry());
    // register the event listener to handle service updates
    Wrapper.getInstance().getEventManager().registerListener(new SyncProxyCloudListener<>(this.syncProxyManagement));
    // register the bungeecord ping & join listener
    this.getProxy().getPluginManager()
      .registerListener(this, new BungeeCordSyncProxyListener(this.syncProxyManagement));
  }

  @Override
  public void onDisable() {
    Wrapper.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());

    this.syncProxyManagement.unregisterService(Wrapper.getInstance().getServicesRegistry());
  }
}
