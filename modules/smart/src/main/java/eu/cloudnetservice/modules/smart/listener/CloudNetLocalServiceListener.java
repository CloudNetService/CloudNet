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

package eu.cloudnetservice.modules.smart.listener;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.modules.smart.CloudNetSmartModule;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;

public final class CloudNetLocalServiceListener {

  private final CloudNetSmartModule module;

  public CloudNetLocalServiceListener(@NonNull CloudNetSmartModule module) {
    this.module = module;
  }

  @EventListener
  public void handle(@NonNull CloudServicePostLifecycleEvent event) {
    if (event.newLifeCycle() == ServiceLifeCycle.PREPARED) {
      var task = CloudNet.instance().serviceTaskProvider()
        .serviceTask(event.service().serviceId().taskName());
      // check if the service is associated with a task
      if (task == null) {
        return;
      }
      // get the smart entry for the service
      var config = this.module.smartConfig(task);
      if (config != null && config.enabled()) {
        Set<ServiceTemplate> templates = new HashSet<>(event.service().waitingTemplates());
        templates.removeAll(task.templates());
        // apply the template installer
        switch (config.templateInstaller()) {
          // installs all templates of the service
          case INSTALL_ALL -> templates.addAll(task.templates());

          // installs a random amount of templates
          case INSTALL_RANDOM -> {
            if (!task.templates().isEmpty()) {
              // get the amount of templates to install
              var amount = ThreadLocalRandom.current().nextInt(1, task.templates().size());
              // install randomly picked templates
              ThreadLocalRandom.current().ints(amount, 0, task.templates().size())
                .forEach(i -> templates.add(Iterables.get(task.templates(), i)));
            }
          }

          // installs one random template
          case INSTALL_RANDOM_ONCE -> {
            if (!task.templates().isEmpty()) {
              // get the template to install
              var index = ThreadLocalRandom.current().nextInt(0, task.templates().size());
              templates.add(Iterables.get(task.templates(), index));
            }
          }

          // installs the templates balanced
          case INSTALL_BALANCED -> {
            var services = CloudNet.instance()
              .cloudServiceProvider()
              .servicesByTask(task.name());
            // find the least used template add register it as a service template
            task.templates().stream()
              .min(Comparator.comparingLong(template -> services.stream()
                .flatMap(service -> service.configuration().templates().stream())
                .filter(template::equals)
                .count()))
              .ifPresent(templates::add);
          }
          default -> {
          }
        }
        // refresh the waiting templates
        event.service().waitingTemplates().clear();
        event.service().waitingTemplates().addAll(templates);
        // include templates and inclusions now if configured so
        if (config.directTemplatesAndInclusionsSetup()) {
          event.service().includeWaitingServiceTemplates();
          event.service().includeWaitingServiceInclusions();
        }
      }
    }
  }
}
