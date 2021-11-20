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
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.WildcardUtil;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.channel.ChannelMessageSender;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLogEntryEvent;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

@CommandAlias("ser")
@CommandPermission("cloudnet.command.service")
@Description("Manages all services in the cluster")
public final class CommandService {

  private static final Logger LOGGER = LogManager.getLogger(CommandService.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  public CommandService() {
    CloudNet.getInstance().getEventManager().registerListener(this);
  }

  @Suggestions("service")
  public List<String> suggestService(CommandContext<CommandSource> $, String input) {
    return CloudNet.getInstance().getCloudServiceProvider().getCloudServices()
      .stream()
      .map(INameable::getName)
      .collect(Collectors.toList());
  }

  @Parser(suggestions = "service")
  public Collection<ServiceInfoSnapshot> wildcardServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();
    Collection<ServiceInfoSnapshot> knownServices = CloudNet.getInstance().getCloudServiceProvider().getCloudServices();
    Collection<ServiceInfoSnapshot> matchedServices = WildcardUtil.filterWildcard(knownServices, name);
    if (matchedServices.isEmpty()) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-service-not-found"));
    }

    return matchedServices;
  }

  @CommandMethod("service|ser list|l")
  public void displayServices(
    CommandSource source,
    @Flag("id") Integer id,
    @Flag("task") String taskName,
    @Flag("group") String groupName,
    @Flag("names") boolean useNamesOnly
  ) {
    Collection<ServiceInfoSnapshot> services = CloudNet.getInstance().getCloudServiceProvider().getCloudServices()
      .stream()
      .filter(service -> id == null || service.getServiceId().getTaskServiceId() == id)
      .filter(service -> taskName == null || service.getServiceId().getTaskName().equalsIgnoreCase(taskName))
      .filter(service -> groupName == null || service.getConfiguration().getGroups().contains(groupName))
      .sorted()
      .collect(Collectors.toList());

    for (ServiceInfoSnapshot serviceInfoSnapshot : services) {
      if (useNamesOnly) {
        source.sendMessage(
          serviceInfoSnapshot.getServiceId().getName() + " | " + serviceInfoSnapshot.getServiceId().getUniqueId());
      } else {
        source.sendMessage(
          "Name: " + serviceInfoSnapshot.getServiceId().getName() +
            " | Lifecycle: " + serviceInfoSnapshot.getLifeCycle() +
            " | " + (serviceInfoSnapshot.isConnected() ? "Connected" : "Not Connected") //+ extension
        );
      }
    }

    source.sendMessage(String.format("=> Showing %d service(s)", services.size()));
  }

  @CommandMethod("service|ser <name>")
  public void displayBasicServiceInfo(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Flag("full") boolean customProperties
  ) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      ServiceInfoSnapshot updatedService = matchedService.provider().forceUpdateServiceInfo();
      this.displayServiceInfo(source, updatedService, customProperties);
    }
  }

  @CommandMethod("service|ser <name> start")
  public void startServices(
    CommandContext<CommandSource> context,
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().start();
    }
  }

  @CommandMethod("service|ser <name> restart")
  public void restartServices(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().restart();
    }
  }

  @CommandMethod("service|ser <name> stop")
  public void stopServices(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().stop();
    }
  }

  @CommandMethod("service|ser <name> copy|cp [template]")
  public void copyService(
    CommandSource source,
    @Argument(value = "name") Collection<ServiceInfoSnapshot> services,
    @Argument("template") ServiceTemplate template,
    @Flag("excludes") @Quoted String excludes
  ) {
    // associate all services with a template
    Collection<Pair<SpecificCloudServiceProvider, ServiceTemplate>> targets = services.stream()
      .map(service -> {
        if (template != null) {
          return new Pair<>(service.provider(), template);
        } else {
          // find a matching template
          return service.getConfiguration().getTemplates().stream()
            .filter(st -> st.getPrefix().equalsIgnoreCase(service.getServiceId().getTaskName()))
            .filter(st -> st.getName().equalsIgnoreCase("default"))
            .map(st -> new Pair<>(service.provider(), st))
            .findFirst()
            .orElse(null);
        }
      })
      .collect(Collectors.toSet());
    // check if we found a result
    if (targets.isEmpty()) {
      source.sendMessage(I18n.trans("command-copy-service-no-default-template"));
      return;
    }

    for (Pair<SpecificCloudServiceProvider, ServiceTemplate> target : targets) {
      target.getFirst().addServiceDeployment(ServiceDeployment.builder()
        .template(target.getSecond())
        .excludes(this.parseExcludes(excludes))
        .build());
      target.getFirst().removeAndExecuteDeployments();
      // send a message for each service we did copy the template of
      //noinspection ConstantConditions
      source.sendMessage(I18n.trans("command-copy-success")
        .replace("%name%", target.getFirst().getServiceInfoSnapshot().getName())
        .replace("%template%", target.getSecond().toString()));
    }
  }

  @CommandMethod("service|ser <name> delete|del")
  public void deleteServices(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().delete();
    }
  }

  @CommandMethod("service|ser <name> toggle")
  public void toggleScreens(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      if (matchedService.provider().toggleScreenEvents(ChannelMessageSender.self(), "service:screen")) {
        source.sendMessage(
          I18n.trans("command-service-toggle-enabled").replace("%name%", matchedService.getName()));
      } else {
        source.sendMessage(
          I18n.trans("command-service-toggle-disabled").replace("%name%", matchedService.getName()));
      }
    }
  }

  @CommandMethod("service|ser <name> includeInclusions")
  public void includeInclusions(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceInclusions();
    }
  }

  @CommandMethod("service|ser <name> includeTemplates")
  public void includeTemplates(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceTemplates();
    }
  }

  @CommandMethod("service|ser <name> deployResources")
  public void deployResources(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().removeAndExecuteDeployments();
    }
  }

  @CommandMethod("service|ser <name> command|cmd <command>")
  public void sendCommand(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Greedy @Argument("command") String command
  ) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().runCommand(command);
    }
  }

  @CommandMethod("service|ser <name> add deployment <deployment>")
  public void addDeployment(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Argument("deployment") ServiceTemplate template
  ) {
    ServiceDeployment deployment = ServiceDeployment.builder().template(template).build();
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().addServiceDeployment(deployment);
    }
  }

  @CommandMethod("service|ser <name> add template <template>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Argument("template") ServiceTemplate template
  ) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().addServiceTemplate(template);
    }
  }

  @CommandMethod("service|ser <name> add inclusion <url> <path>")
  public void addInclusion(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Argument("url") String url,
    @Argument("path") String path
  ) {
    ServiceRemoteInclusion remoteInclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().addServiceRemoteInclusion(remoteInclusion);
    }
  }

  @EventListener(channel = "service:screen")
  public void handleLogEntry(CloudServiceLogEntryEvent event) {
    LOGGER.info(String.format("&b[%s] %s", event.getServiceInfo().getName(), event.getLine()));
  }

  private void displayServiceInfo(CommandSource source, @Nullable ServiceInfoSnapshot service,
    boolean showCustomProperties) {
    if (service == null) {
      return;
    }

    Collection<String> list = new ArrayList<>(Arrays.asList(
      " ",
      "* CloudService: " + service.getServiceId().getUniqueId().toString(),
      "* Name: " + service.getServiceId().getName(),
      "* Node: " + service.getServiceId().getNodeUniqueId(),
      "* Address: " + service.getAddress().getHost() + ":" + service.getAddress().getPort()
    ));

    if (service.getServiceId().getEnvironment().isMinecraftServer()
      && !service.getAddress().getHost().equals(service.getConnectAddress().getHost())) {
      list.add(
        "* Address for connections: " + service.getConnectAddress().getHost() + ":" + service
          .getConnectAddress().getPort());
    }

    if (service.isConnected()) {
      list.add("* Connected: " + DATE_FORMAT.format(service.getConnectedTime()));
    } else {
      list.add("* Connected: false");
    }

    list.add("* Lifecycle: " + service.getLifeCycle());
    list.add("* Groups: " + String.join(", ", service.getConfiguration().getGroups()));

    if (!service.getConfiguration().getIncludes().isEmpty()) {
      list.add(" ");
      list.add("* Includes:");

      for (ServiceRemoteInclusion inclusion : service.getConfiguration().getIncludes()) {
        list.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
      }
    }

    if (!service.getConfiguration().getTemplates().isEmpty()) {
      list.add(" ");
      list.add("* Templates:");

      for (ServiceTemplate template : service.getConfiguration().getTemplates()) {
        list.add("- " + template);
      }
    }

    if (!service.getConfiguration().getDeployments().isEmpty()) {
      list.add(" ");
      list.add("* Deployments:");

      for (ServiceDeployment deployment : service.getConfiguration().getDeployments()) {
        list.add("- ");
        list
          .add(
            "Template:  " + deployment.getTemplate());
        list.add("Excludes: " + deployment.getExcludes());
      }
    }

    list.add(" ");
    list.add("* ServiceInfoSnapshot | " + DATE_FORMAT.format(service.getCreationTime()));

    list.addAll(Arrays.asList(
      "PID: " + service.getProcessSnapshot().getPid(),
      "CPU usage: " + CPUUsageResolver.FORMAT
        .format(service.getProcessSnapshot().getCpuUsage()) + "%",
      "Threads: " + service.getProcessSnapshot().getThreads().size(),
      "Heap usage: " + (service.getProcessSnapshot().getHeapUsageMemory() / 1048576) + "/" +
        (service.getProcessSnapshot().getMaxHeapMemory() / 1048576) + "MB",
      " "
    ));

    if (showCustomProperties) {
      list.add("Properties:");
      list.addAll(Arrays.asList(service.getProperties().toPrettyJson().split("\n")));
      list.add(" ");
    }

    source.sendMessage(list);
  }

  private Collection<String> parseExcludes(@Nullable String excludes) {
    if (excludes == null) {
      return Collections.emptyList();
    }

    return Arrays.asList(excludes.split(";"));
  }
}
