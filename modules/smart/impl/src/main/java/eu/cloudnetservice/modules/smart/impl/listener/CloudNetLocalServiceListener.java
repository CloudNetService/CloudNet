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

package eu.cloudnetservice.modules.smart.impl.listener;

import com.google.common.collect.Iterables;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.modules.smart.SmartServiceTaskConfig;
import eu.cloudnetservice.modules.smart.impl.CloudNetSmartModule;
import eu.cloudnetservice.node.event.service.CloudServicePostLifecycleEvent;
import eu.cloudnetservice.node.event.service.CloudServicePrePrepareEvent;
import eu.cloudnetservice.node.service.CloudService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;

@Singleton
public final class CloudNetLocalServiceListener {

  private final CloudNetSmartModule module;
  private final ServiceTaskProvider taskProvider;
  private final CloudServiceProvider cloudServiceProvider;

  @Inject
  public CloudNetLocalServiceListener(
    @NonNull CloudNetSmartModule module,
    @NonNull ServiceTaskProvider taskProvider,
    @NonNull CloudServiceProvider cloudServiceProvider
  ) {
    this.module = module;
    this.taskProvider = taskProvider;
    this.cloudServiceProvider = cloudServiceProvider;
  }

  @EventListener
  public void handle(@NonNull CloudServicePostLifecycleEvent event) {
    if (event.newLifeCycle() == ServiceLifeCycle.PREPARED) {
      // check if the service is associated with a task
      var task = this.taskProvider.serviceTask(event.service().serviceId().taskName());
      if (task == null) {
        return;
      }

      // include templates and inclusions if configured
      var config = this.module.smartConfig(task);
      if (config != null && config.enabled() && config.directTemplatesAndInclusionsSetup()) {
        this.installTemplates(config, task, event.service());

        // add the initial inclusions of the service
        event.service().waitingIncludes().addAll(event.service().serviceConfiguration().inclusions());

        // include the waiting templates now, force the inclusion if the service gets started for the first time
        var firstStartup = Files.notExists(event.service().directory());
        event.service().includeWaitingServiceTemplates(firstStartup);

        // include all waiting inclusions
        event.service().includeWaitingServiceInclusions();
      }
    }
  }

  @EventListener
  public void handlePrePrepare(@NonNull CloudServicePrePrepareEvent event) {
    // check if the service is associated with a task
    var task = this.taskProvider.serviceTask(event.service().serviceId().taskName());
    if (task == null) {
      return;
    }

    var config = this.module.smartConfig(task);
    if (config != null && config.enabled()) {
      if (config.directTemplatesAndInclusionsSetup()) {
        // remove all initial templates & inclusions from the waiting templates, as they
        // were included during the PREPARED state of the service already
        event.service().waitingTemplates().removeAll(event.service().serviceConfiguration().templates());
        event.service().waitingIncludes().removeAll(event.service().serviceConfiguration().inclusions());
      } else {
        this.installTemplates(config, task, event.service());
      }
    }
  }

  private void installTemplates(
    @NonNull SmartServiceTaskConfig config,
    @NonNull ServiceTask task,
    @NonNull CloudService service
  ) {
    // get the smart entry for the service
    Set<ServiceTemplate> templates = new HashSet<>(service.serviceConfiguration().templates());
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
        var services = this.cloudServiceProvider.servicesByTask(task.name());
        // find the least used template add register it as a service template
        task.templates().stream()
          .min(Comparator.comparingLong(template -> services.stream()
            .flatMap(snapshot -> snapshot.provider().installedTemplates().stream())
            .filter(template::equals)
            .count()))
          .ifPresent(templates::add);
      }

      default -> {
      }
    }

    // refresh the waiting templates
    service.waitingTemplates().clear();
    service.waitingTemplates().addAll(templates);
  }
}
