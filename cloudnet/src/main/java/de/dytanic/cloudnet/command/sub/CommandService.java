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

import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.event.ServiceListCommandEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

public class CommandService {

  @Parser
  public ServiceInfoSnapshot defaultServiceParser(CommandContext<CommandSource> sender, Queue<String> input) {
    String name = input.remove();
    ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceManager()
      .getCloudServiceByName(name);
    if (serviceInfoSnapshot == null) {
      throw new ArgumentNotAvailableException("No service found");
    }
    return serviceInfoSnapshot;
  }

  @CommandMethod("service|ser list|l")
  public void displayServices(
    CommandSource source,
    @Flag("id") Integer id,
    @Flag("task") String taskName,
    @Flag("group") String groupName,
    @Flag("names") boolean useNamesOnly
  ) {
    Collection<ServiceInfoSnapshot> services = CloudNet.getInstance().getCloudServiceManager().getCloudServices()
      .stream()
      .filter(service -> id == null || service.getServiceId().getTaskServiceId() == id)
      .filter(service -> taskName == null || service.getServiceId().getTaskName().equalsIgnoreCase(taskName))
      .filter(service -> groupName == null || Arrays.asList(service.getConfiguration().getGroups()).contains(groupName))
      .sorted()
      .collect(Collectors.toList());

    ServiceListCommandEvent event = CloudNet.getInstance().getEventManager()
      .callEvent(new ServiceListCommandEvent(services));
    for (ServiceInfoSnapshot serviceInfoSnapshot : services) {
      String extension = event.getAdditionalParameters()
        .stream()
        .map(function -> function.apply(serviceInfoSnapshot))
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" | "));
      if (!extension.isEmpty()) {
        extension = " | " + extension;
      }

      if (useNamesOnly) {
        source.sendMessage(
          serviceInfoSnapshot.getServiceId().getName() + " | " + serviceInfoSnapshot.getServiceId().getUniqueId());
      } else {
        source.sendMessage(
          "Name: " + serviceInfoSnapshot.getServiceId().getName() +
            " | Node: " + serviceInfoSnapshot.getServiceId().getNodeUniqueId() +
            " | Status: " + serviceInfoSnapshot.getLifeCycle() +
            " | Address: " + serviceInfoSnapshot.getAddress().getHost() + ":" + serviceInfoSnapshot.getAddress()
            .getPort() + " | " + (serviceInfoSnapshot.isConnected() ? "Connected" : "Not Connected") + extension
        );
      }

      StringBuilder builder = new StringBuilder(
        String.format("=> Showing %d service(s)", services.size()));
      for (String parameter : event.getAdditionalSummary()) {
        builder.append("; ").append(parameter);
      }
      source.sendMessage(builder.toString());
    }

  }

}
