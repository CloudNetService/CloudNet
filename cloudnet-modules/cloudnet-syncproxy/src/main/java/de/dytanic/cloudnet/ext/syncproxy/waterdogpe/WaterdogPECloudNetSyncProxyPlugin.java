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

package de.dytanic.cloudnet.ext.syncproxy.waterdogpe;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyCloudNetListener;
import de.dytanic.cloudnet.ext.syncproxy.waterdogpe.listener.WaterdogPESyncProxyPlayerListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import dev.waterdog.waterdogpe.plugin.Plugin;

public class WaterdogPECloudNetSyncProxyPlugin extends Plugin {

  @Override
  public void onEnable() {
    // register management
    WaterdogPESyncProxyManagement management = new WaterdogPESyncProxyManagement();
    CloudNetDriver.getInstance().getServicesRegistry()
      .registerService(AbstractSyncProxyManagement.class, "WaterdogPESyncProxyManagement", management);
    // cloud listener
    CloudNetDriver.getInstance().getEventManager().registerListener(new SyncProxyCloudNetListener(management));
    // platform listener
    new WaterdogPESyncProxyPlayerListener(management);
  }

  @Override
  public void onDisable() {
    CloudNetDriver.getInstance().getEventManager().unregisterListeners(this.getClass().getClassLoader());
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(this.getClass().getClassLoader());

    CloudNetDriver.getInstance().getServicesRegistry()
      .unregisterService(AbstractSyncProxyManagement.class, "WaterdogPESyncProxyManagement");
  }
}
