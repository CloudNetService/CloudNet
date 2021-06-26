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

package de.dytanic.cloudnet.examples.plugin;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.examples.ExampleListener;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitPluginExample extends JavaPlugin {

  @Override
  public void onEnable() {
    ServiceId serviceId = Wrapper.getInstance().getServiceId();

    serviceId.getUniqueId(); //The unique identifier of the service
    serviceId.getTaskName(); //The task from the service
    serviceId.getTaskServiceId(); //The id of a service by a task
    serviceId.getNodeUniqueId(); //The Id of the node process, which the service provided is
    serviceId.getEnvironment(); //The application environment type, which is configured for this service

    ServiceConfiguration serviceConfiguration = Wrapper.getInstance()
      .getServiceConfiguration(); //The own serviceConfiguration instance

    CloudNetDriver.getInstance().getEventManager().registerListener(new ExampleListener());
  }

  @Override
  public void onDisable() { // Important! Remove all your own registered listeners or registry items
    CloudNetDriver.getInstance().getEventManager()
      .unregisterListeners(this.getClassLoader()); //Removes all Event listeners from this plugin
    Wrapper.getInstance().getServicesRegistry()
      .unregisterAll(this.getClassLoader()); //Removes all ServiceRegistry items if they exists
    Wrapper.getInstance().unregisterPacketListenersByClassLoader(
      this.getClassLoader()); //Removes all IPacketListener implementations on all channels from the network connector
  }
}
