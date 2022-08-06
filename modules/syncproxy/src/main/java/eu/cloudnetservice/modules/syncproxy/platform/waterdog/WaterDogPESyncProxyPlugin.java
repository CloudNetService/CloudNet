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

package eu.cloudnetservice.modules.syncproxy.platform.waterdog;

import dev.waterdog.waterdogpe.plugin.Plugin;
import eu.cloudnetservice.driver.util.ModuleUtil;
import eu.cloudnetservice.modules.syncproxy.platform.listener.SyncProxyCloudListener;
import eu.cloudnetservice.wrapper.Wrapper;

public final class WaterDogPESyncProxyPlugin extends Plugin {

  @Override
  public void onEnable() {
    var syncProxyManagement = new WaterDogPESyncProxyManagement(this.getProxy());
    // register the SyncProxyManagement in our service registry
    syncProxyManagement.registerService(Wrapper.instance().serviceRegistry());
    // register the event listener to handle service updates
    Wrapper.instance().eventManager().registerListener(new SyncProxyCloudListener<>(syncProxyManagement));
    // register the waterdog ping & join listener
    new WaterDogPESyncProxyListener(syncProxyManagement, this.getProxy());
  }

  @Override
  public void onDisable() {
    // unregister all listeners for cloudnet events
    ModuleUtil.unregisterAll(this.getClass().getClassLoader());
  }
}
