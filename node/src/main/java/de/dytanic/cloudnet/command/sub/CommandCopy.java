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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.Quoted;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;

@CommandAlias("cp")
@CommandPermission("cloudnet.command.copy")
@Description("Copies a running service to a specific template")
public final class CommandCopy {

  @CommandMethod("copy|cp <service> [template]")
  public void copyService(
    CommandSource source,
    @Argument(value = "service", parserName = "single") ServiceInfoSnapshot service,
    @Argument("template") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes
  ) {
    ServiceTemplate targetTemplate = template;
    if (template == null) {
      for (ServiceTemplate serviceTemplate : service.getConfiguration().getTemplates()) {
        if (!serviceTemplate.getPrefix().equalsIgnoreCase(service.getServiceId().getTaskName())) {
          continue;
        }

        if (!serviceTemplate.getName().equalsIgnoreCase("default")) {
          continue;
        }

        targetTemplate = serviceTemplate;
        break;
      }
    }

    if (template == null) {
      source.sendMessage(I18n.trans("command-copy-service-no-default-template"));
      return;
    }

    SpecificCloudServiceProvider serviceProvider = service.provider();

    List<ServiceDeployment> oldDeployments = new ArrayList<>(service.getConfiguration().getDeployments());

    serviceProvider.addServiceDeployment(new ServiceDeployment(targetTemplate, this.parseExcludes(excludes)));
    serviceProvider.deployResources(true);

    for (ServiceDeployment deployment : oldDeployments) {
      serviceProvider.addServiceDeployment(deployment);
    }

    source.sendMessage(
      I18n.trans("command-copy-success")
        .replace("%name%", service.getServiceId().getName())
        .replace("%template%",
          targetTemplate.getStorage() + ":" + targetTemplate.getPrefix() + "/" + targetTemplate.getName())
    );
  }

  private Collection<String> parseExcludes(@Nullable String excludes) {
    if (excludes == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(excludes.split(";"));
  }

}
