/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.smart.impl;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import eu.cloudnetservice.modules.smart.impl.listener.CloudNetLocalServiceListener;
import eu.cloudnetservice.modules.smart.impl.listener.CloudNetLocalServiceTaskListener;
import eu.cloudnetservice.modules.smart.impl.listener.CloudNetTickListener;
import eu.cloudnetservice.node.command.CommandProvider;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class CloudNetSmartModule extends DriverModule {

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED, order = Byte.MAX_VALUE)
  public void rewriteOldSmartTaskEntries(@NonNull ServiceTaskProvider taskProvider) {
    for (var task : taskProvider.serviceTasks()) {
      // check if the task had a smart config entry previously
      if (task.propertyHolder().contains("smartConfig")) {
        // check if the task still uses the old format
        var smartEntry = task.propertyHolder().readDocument("smartConfig");
        if (smartEntry.contains("dynamicMemoryAllocationRange")) {
          // rewrite the old config
          var config = SmartServiceTaskConfig.builder()
            .enabled(smartEntry.getBoolean("enabled"))
            .priority(smartEntry.getInt("priority"))

            .maxServices(smartEntry.getInt("maxServiceCount"))
            .preparedServices(smartEntry.getInt("preparedServices"))

            .directTemplatesAndInclusionsSetup(smartEntry.getBoolean("directTemplatesAndInclusionsSetup"))
            .templateInstaller(
              smartEntry.readObject("templateInstaller", SmartServiceTaskConfig.TemplateInstaller.class))

            .autoStopTimeByUnusedServiceInSeconds(smartEntry.getInt("autoStopTimeByUnusedServiceInSeconds"))
            .percentOfPlayersToCheckShouldStop(
              smartEntry.getInt("percentOfPlayersToCheckShouldAutoStopTheServiceInFuture"))

            .forAnewInstanceDelayTimeInSeconds(smartEntry.getInt("forAnewInstanceDelayTimeInSeconds"))
            .percentOfPlayersForANewServiceByInstance(smartEntry.getInt("percentOfPlayersForANewServiceByInstance"))

            .build();

          // append the new smart entry and update the service
          var newTask = ServiceTask.builder(task)
            .modifyProperties(properties -> properties.append("smartConfig", config))
            .build();
          taskProvider.addServiceTask(newTask);
        }
      }
    }
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED, order = 64)
  public void addMissingSmartConfigurationEntries(@NonNull ServiceTaskProvider taskProvider) {
    for (var task : taskProvider.serviceTasks()) {
      // check if the service task needs a smart entry
      if (!task.propertyHolder().contains("smartConfig")) {
        var newTask = ServiceTask.builder(task)
          .modifyProperties(properties -> properties.append("smartConfig", SmartServiceTaskConfig.builder().build()))
          .build();
        taskProvider.addServiceTask(newTask);
      }
    }
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
  public void start(@NonNull EventManager eventManager, @NonNull CommandProvider commandProvider) {
    eventManager
      .registerListener(CloudNetTickListener.class)
      .registerListener(CloudNetLocalServiceListener.class)
      .registerListener(CloudNetLocalServiceTaskListener.class);

    commandProvider.register(SmartCommand.class);
  }

  public @Nullable SmartServiceTaskConfig smartConfig(@NonNull ServiceTask task) {
    // try to get the smart config entry
    return task.propertyHolder().readObject("smartConfig", SmartServiceTaskConfig.class);
  }
}
