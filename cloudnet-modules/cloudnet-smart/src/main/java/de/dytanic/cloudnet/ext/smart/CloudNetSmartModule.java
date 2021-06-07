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

package de.dytanic.cloudnet.ext.smart;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.smart.listener.CloudNetTickListener;
import de.dytanic.cloudnet.ext.smart.listener.CloudServiceListener;
import de.dytanic.cloudnet.ext.smart.listener.TaskDefaultSmartConfigListener;
import de.dytanic.cloudnet.ext.smart.template.TemplateInstaller;
import de.dytanic.cloudnet.ext.smart.util.SmartServiceTaskConfig;
import de.dytanic.cloudnet.module.NodeCloudNetModule;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CloudNetSmartModule extends NodeCloudNetModule {

  public static final String SMART_CONFIG_ENTRY = "smartConfig";

  private static final Random RANDOM = new Random();

  private static CloudNetSmartModule instance;

  private final Map<UUID, CloudNetServiceSmartProfile> providedSmartServices = new ConcurrentHashMap<>();

  public CloudNetSmartModule() {
    instance = this;
  }

  public static CloudNetSmartModule getInstance() {
    return CloudNetSmartModule.instance;
  }

  @ModuleTask(order = 127, event = ModuleLifeCycle.STARTED)
  public void initTaskConfigs() {
    Map<String, SmartServiceTaskConfig> oldSmartTasks = new HashMap<>();
    if (Files.exists(this.getModuleWrapper().getDataDirectory()) && this.getConfig().contains("smartTasks")) {
      Collection<JsonDocument> smartTasks = this.getConfig()
        .get("smartTasks", new TypeToken<Collection<JsonDocument>>() {
        }.getType());
      for (JsonDocument smartTaskJson : smartTasks) {
        SmartServiceTaskConfig smartTask = smartTaskJson.toInstanceOf(SmartServiceTaskConfig.class);
        smartTask.setEnabled(true);
        oldSmartTasks.put(
          smartTaskJson.getString("task"),
          smartTask
        );
      }
      FileUtils.delete(this.getModuleWrapper().getDataDirectory());
    }

    for (ServiceTask task : this.getCloudNet().getServiceTaskProvider().getPermanentServiceTasks()) {
      SmartServiceTaskConfig config = task.getProperties().get(SMART_CONFIG_ENTRY, SmartServiceTaskConfig.class);
      if (config == null) {
        task.getProperties().append(
          SMART_CONFIG_ENTRY,
          oldSmartTasks.containsKey(task.getName()) ?
            oldSmartTasks.get(task.getName()) :
            new SmartServiceTaskConfig()
        );
        this.getCloudNet().getServiceTaskProvider().addPermanentServiceTask(task);
      }
    }
  }

  @ModuleTask(order = 16, event = ModuleLifeCycle.STARTED)
  public void initListeners() {
    this
      .registerListeners(new CloudNetTickListener(), new CloudServiceListener(), new TaskDefaultSmartConfigListener());
  }

  public SmartServiceTaskConfig getSmartServiceTaskConfig(ServiceTask task) {
    return task.getProperties().get(SMART_CONFIG_ENTRY, SmartServiceTaskConfig.class);
  }

  public SmartServiceTaskConfig getSmartServiceTaskConfig(ServiceInfoSnapshot serviceInfoSnapshot) {
    ServiceTask task = this.getCloudNet().getServiceTaskProvider()
      .getServiceTask(serviceInfoSnapshot.getServiceId().getTaskName());
    return task != null ? this.getSmartServiceTaskConfig(task) : null;
  }

  public boolean hasSmartServiceTaskConfig(ServiceTask task) {
    return task.getProperties().contains(SMART_CONFIG_ENTRY) && this.getSmartServiceTaskConfig(task).isEnabled();
  }

  public int getPercentOfFreeMemory(String nodeId, ServiceTask serviceTask) {
    NetworkClusterNodeInfoSnapshot networkClusterNodeInfoSnapshot = CloudNet.getInstance().getNodeInfoProvider()
      .getNodeInfoSnapshot(nodeId);

    if (networkClusterNodeInfoSnapshot != null && !networkClusterNodeInfoSnapshot.getNode()
      .getUniqueId().equalsIgnoreCase(this.getCloudNetConfig().getIdentity().getUniqueId())) {
      int memory =
        (networkClusterNodeInfoSnapshot.getMaxMemory() - networkClusterNodeInfoSnapshot.getUsedMemory()) * 100;
      return networkClusterNodeInfoSnapshot.getMaxMemory() > 0 ? memory / networkClusterNodeInfoSnapshot.getMaxMemory()
        : memory;
    } else {
      int memory =
        (this.getCloudNet().getConfig().getMaxMemory() - this.getCloudNet().getCurrentNetworkClusterNodeInfoSnapshot()
          .getUsedMemory()) * 100;
      return this.getCloudNet().getConfig().getMaxMemory() > 0 ? memory / this.getCloudNet().getConfig().getMaxMemory()
        : memory;
    }
  }

  public ServiceInfoSnapshot getFreeNonStartedService(String taskName) {
    return CloudNet.getInstance().getCloudServiceProvider().getCloudServices().stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getTaskName().equalsIgnoreCase(taskName) &&
        (serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.PREPARED
          || serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.DEFINED))
      .findFirst()
      .orElse(null);
  }

  public void updateAsSmartService(ServiceConfiguration configuration, ServiceTask serviceTask,
    SmartServiceTaskConfig smartTask) {
    configuration.setTemplates(this.applyTemplateInstaller(configuration, new ArrayList<>(serviceTask.getTemplates()),
      smartTask.getTemplateInstaller()));
    configuration.setInitTemplates(configuration.getTemplates());
    configuration.getProcessConfig().setMaxHeapMemorySize(
      this.applyDynamicMemory(serviceTask.getProcessConfiguration().getMaxHeapMemorySize(), configuration, serviceTask,
        smartTask)
    );
  }

  private ServiceTemplate[] applyTemplateInstaller(ServiceConfiguration configuration,
    List<ServiceTemplate> taskTemplates, TemplateInstaller templateInstaller) {
    List<ServiceTemplate> outTemplates = new ArrayList<>(Arrays.asList(configuration.getTemplates()));
    outTemplates.removeAll(taskTemplates);

    switch (templateInstaller) {
      case INSTALL_ALL: {
        outTemplates.addAll(taskTemplates);
        break;
      }
      case INSTALL_RANDOM: {
        if (!taskTemplates.isEmpty()) {
          int size = RANDOM.nextInt(taskTemplates.size());

          for (int i = 0; i < size; ++i) {
            ServiceTemplate item = taskTemplates.get(RANDOM.nextInt(taskTemplates.size()));
            taskTemplates.remove(item);
            outTemplates.add(item);
          }
        }
        break;
      }
      case INSTALL_RANDOM_ONCE: {
        if (!taskTemplates.isEmpty()) {
          ServiceTemplate item = taskTemplates.get(RANDOM.nextInt(taskTemplates.size()));
          outTemplates.add(item);
        }
        break;
      }
      case INSTALL_BALANCED: {
        if (!taskTemplates.isEmpty()) {
          Collection<ServiceInfoSnapshot> services = super.getCloudNet().getCloudServiceProvider()
            .getCloudServices(configuration.getServiceId().getTaskName());
          taskTemplates.stream()
            .min(Comparator.comparingLong(serviceTemplate ->
              services.stream()
                .map(serviceInfoSnapshot -> serviceInfoSnapshot.getConfiguration().getInitTemplates())
                .flatMap(Arrays::stream)
                .filter(serviceTemplate::equals)
                .count()
            ))
            .ifPresent(outTemplates::add);
        }

        break;
      }
      default:
        break;
    }

    return outTemplates.toArray(new ServiceTemplate[0]);
  }

  private int applyDynamicMemory(int maxMemory, ServiceConfiguration serviceConfiguration, ServiceTask serviceTask,
    SmartServiceTaskConfig smartTask) {
    if (smartTask.isDynamicMemoryAllocation()) {
      int percent = this.getPercentOfFreeMemory(serviceConfiguration.getServiceId().getNodeUniqueId(), serviceTask);

      if (percent > 50) {
        maxMemory = maxMemory - ((percent * smartTask.getDynamicMemoryAllocationRange()) / 100);
      } else {
        maxMemory = maxMemory + ((percent * smartTask.getDynamicMemoryAllocationRange()) / 100);
      }
    }

    return maxMemory;
  }


  public Map<UUID, CloudNetServiceSmartProfile> getProvidedSmartServices() {
    return this.providedSmartServices;
  }

}
