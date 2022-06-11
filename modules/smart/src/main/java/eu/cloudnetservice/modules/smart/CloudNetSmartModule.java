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

package eu.cloudnetservice.modules.smart;

import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.smart.listener.CloudNetLocalServiceListener;
import eu.cloudnetservice.modules.smart.listener.CloudNetLocalServiceTaskListener;
import eu.cloudnetservice.modules.smart.listener.CloudNetTickListener;
import eu.cloudnetservice.node.Node;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class CloudNetSmartModule extends DriverModule {

  @ModuleTask(event = ModuleLifeCycle.STARTED, order = Byte.MAX_VALUE)
  public void rewriteOldSmartTaskEntries() {
    for (var task : this.driver().serviceTaskProvider().serviceTasks()) {
      // check if the task had a smart config entry previously
      if (task.properties().contains("smartConfig")) {
        var smartEntry = task.properties().getDocument("smartConfig");
        // check if the task still uses the old format
        if (smartEntry.contains("dynamicMemoryAllocationRange")) {
          // rewrite the old config
          var config = SmartServiceTaskConfig.builder()
            .enabled(smartEntry.getBoolean("enabled"))
            .priority(smartEntry.getInt("priority"))

            .maxServices(smartEntry.getInt("maxServiceCount"))
            .preparedServices(smartEntry.getInt("preparedServices"))

            .templateInstaller(smartEntry.get("templateInstaller", SmartServiceTaskConfig.TemplateInstaller.class))
            .directTemplatesAndInclusionsSetup(smartEntry.getBoolean("directTemplatesAndInclusionsSetup"))

            .autoStopTimeByUnusedServiceInSeconds(smartEntry.getInt("autoStopTimeByUnusedServiceInSeconds"))
            .percentOfPlayersToCheckShouldStop(
              smartEntry.getInt("percentOfPlayersToCheckShouldAutoStopTheServiceInFuture"))

            .forAnewInstanceDelayTimeInSeconds(smartEntry.getInt("forAnewInstanceDelayTimeInSeconds"))
            .percentOfPlayersForANewServiceByInstance(smartEntry.getInt("percentOfPlayersForANewServiceByInstance"))

            .build();
          // append the new smart entry and update the service
          task.properties().append("smartConfig", config);
          this.driver().serviceTaskProvider().addServiceTask(task);
        }
      }
    }
  }

  @ModuleTask(event = ModuleLifeCycle.STARTED, order = 64)
  public void addMissingSmartConfigurationEntries() {
    for (var task : this.driver().serviceTaskProvider().serviceTasks()) {
      // check if the service task needs a smart entry
      if (!task.properties().contains("smartConfig")) {
        task.properties().append("smartConfig", SmartServiceTaskConfig.builder().build());
        // update the task
        Node.instance().serviceTaskProvider().addServiceTask(task);
      }
    }
  }

  @ModuleTask(event = ModuleLifeCycle.STARTED)
  public void start() {
    this.registerListener(
      new CloudNetTickListener(this),
      new CloudNetLocalServiceTaskListener(),
      new CloudNetLocalServiceListener(this));

    Node.instance().commandProvider().register(new SmartCommand());
  }

  public @Nullable SmartServiceTaskConfig smartConfig(@NonNull ServiceTask task) {
    // try to get the smart config entry
    return task.properties().get("smartConfig", SmartServiceTaskConfig.class);
  }
}
