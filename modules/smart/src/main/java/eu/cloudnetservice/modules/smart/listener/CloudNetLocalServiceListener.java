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

package eu.cloudnetservice.modules.smart.listener;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.modules.smart.CloudNetSmartModule;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;

public final class CloudNetLocalServiceListener {

  private final CloudNetSmartModule module;

  public CloudNetLocalServiceListener(@NotNull CloudNetSmartModule module) {
    this.module = module;
  }

  @EventListener
  public void handle(@NotNull CloudServicePostLifecycleEvent event) {
    if (event.getNewLifeCycle() == ServiceLifeCycle.PREPARED) {
      ServiceTask task = CloudNet.getInstance().getServiceTaskProvider()
        .getServiceTask(event.getService().getServiceId().getTaskName());
      // check if the service is associated with a task
      if (task == null) {
        return;
      }
      // get the smart entry for the service
      SmartServiceTaskConfig config = this.module.getSmartConfig(task);
      if (config != null && config.isEnabled()) {
        Set<ServiceTemplate> templates = new HashSet<>(event.getService().getWaitingTemplates());
        templates.removeAll(task.getTemplates());
        // apply the template installer
        switch (config.getTemplateInstaller()) {
          // installs all templates of the service
          case INSTALL_ALL: {
            templates.addAll(task.getTemplates());
          }
          break;
          // installs a random amount of templates
          case INSTALL_RANDOM: {
            if (!task.getTemplates().isEmpty()) {
              // get the amount of templates to install
              int amount = ThreadLocalRandom.current().nextInt(1, task.getTemplates().size());
              // install randomly picked templates
              ThreadLocalRandom.current().ints(amount, 0, task.getTemplates().size())
                .forEach(i -> templates.add(Iterables.get(task.getTemplates(), i)));
            }
          }
          break;
          // installs one random template
          case INSTALL_RANDOM_ONCE: {
            if (!task.getTemplates().isEmpty()) {
              // get the template to install
              int index = ThreadLocalRandom.current().nextInt(0, task.getTemplates().size());
              templates.add(Iterables.get(task.getTemplates(), index));
            }
          }
          break;
          // installs the templates balanced
          case INSTALL_BALANCED: {
            Collection<ServiceInfoSnapshot> services = CloudNet.getInstance()
              .getCloudServiceProvider()
              .getCloudServicesByTask(task.getName());
            // find the least used template add register it as a service template
            task.getTemplates().stream()
              .min(Comparator.comparingLong(template -> services.stream()
                .flatMap(service -> service.getConfiguration().getTemplates().stream())
                .filter(template::equals)
                .count()))
              .ifPresent(templates::add);
          }
          break;
          default:
            break;
        }
        // refresh the waiting templates
        event.getService().getWaitingTemplates().clear();
        event.getService().getWaitingTemplates().addAll(templates);
        // include templates and inclusions now if configured so
        if (config.isDirectTemplatesAndInclusionsSetup()) {
          event.getService().includeWaitingServiceTemplates();
          event.getService().includeWaitingServiceInclusions();
        }
      }
    }
  }
}
