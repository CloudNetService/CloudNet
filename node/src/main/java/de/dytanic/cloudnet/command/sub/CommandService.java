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
import de.dytanic.cloudnet.common.Nameable;
import de.dytanic.cloudnet.common.WildcardUtil;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.column.ColumnFormatter;
import de.dytanic.cloudnet.common.column.RowBasedFormatter;
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

  private static final Logger LOGGER = LogManager.logger(CommandService.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  // there are different ways to display the services
  private static final RowBasedFormatter<ServiceInfoSnapshot> NAMES_ONLY = RowBasedFormatter.<ServiceInfoSnapshot>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "UID").build())
    .column(ServiceInfoSnapshot::name)
    .column(service -> service.serviceId().uniqueId())
    .build();
  private static final RowBasedFormatter<ServiceInfoSnapshot> SERVICES = RowBasedFormatter.<ServiceInfoSnapshot>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "Lifecycle", "Node", "State").build())
    .column(ServiceInfoSnapshot::name)
    .column(ServiceInfoSnapshot::lifeCycle)
    .column(service -> service.serviceId().nodeUniqueId())
    .column(service -> service.connected() ? "Connected" : "Not connected")
    .build();

  public CommandService() {
    CloudNet.instance().eventManager().registerListener(this);
  }

  @Suggestions("service")
  public List<String> suggestService(CommandContext<CommandSource> $, String input) {
    return CloudNet.instance().cloudServiceProvider().services()
      .stream()
      .map(Nameable::name)
      .toList();
  }

  @Parser(suggestions = "service")
  public Collection<ServiceInfoSnapshot> wildcardServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    var name = input.remove();
    var knownServices = CloudNet.instance().cloudServiceProvider().services();
    var matchedServices = WildcardUtil.filterWildcard(knownServices, name);
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
    Collection<ServiceInfoSnapshot> services = CloudNet.instance().cloudServiceProvider().services()
      .stream()
      .filter(service -> id == null || service.serviceId().taskServiceId() == id)
      .filter(service -> taskName == null || service.serviceId().taskName().equalsIgnoreCase(taskName))
      .filter(service -> groupName == null || service.configuration().groups().contains(groupName))
      .sorted()
      .toList();

    // there are different ways to list services
    if (useNamesOnly) {
      source.sendMessage(NAMES_ONLY.format(services));
    } else {
      source.sendMessage(SERVICES.format(services));
    }

    source.sendMessage(String.format("=> Showing %d service(s)", services.size()));
  }

  @CommandMethod("service|ser <name>")
  public void displayBasicServiceInfo(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Flag("full") boolean customProperties
  ) {
    for (var matchedService : matchedServices) {
      var updatedService = matchedService.provider().forceUpdateServiceInfo();
      this.displayServiceInfo(source, updatedService, customProperties);
    }
  }

  @CommandMethod("service|ser <name> start")
  public void startServices(
    CommandContext<CommandSource> context,
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().start();
    }
  }

  @CommandMethod("service|ser <name> restart")
  public void restartServices(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (var matchedService : matchedServices) {
      matchedService.provider().restart();
    }
  }

  @CommandMethod("service|ser <name> stop")
  public void stopServices(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (var matchedService : matchedServices) {
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
          return service.configuration().templates().stream()
            .filter(st -> st.prefix().equalsIgnoreCase(service.serviceId().taskName()))
            .filter(st -> st.name().equalsIgnoreCase("default"))
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

    for (var target : targets) {
      target.first().addServiceDeployment(ServiceDeployment.builder()
        .template(target.second())
        .excludes(this.parseExcludes(excludes))
        .build());
      target.first().removeAndExecuteDeployments();
      // send a message for each service we did copy the template of
      //noinspection ConstantConditions
      source.sendMessage(I18n.trans("command-copy-success")
        .replace("%name%", target.first().serviceInfo().name())
        .replace("%template%", target.second().toString()));
    }
  }

  @CommandMethod("service|ser <name> delete|del")
  public void deleteServices(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (var matchedService : matchedServices) {
      matchedService.provider().delete();
    }
  }

  @CommandMethod("service|ser <name> toggle")
  public void toggleScreens(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (var matchedService : matchedServices) {
      if (matchedService.provider().toggleScreenEvents(ChannelMessageSender.self(), "service:screen")) {
        for (var cachedLogMessage : matchedService.provider().cachedLogMessages()) {
          LOGGER.info(String.format("&b[%s] %s", matchedService.name(), cachedLogMessage));
        }
        source.sendMessage(
          I18n.trans("command-service-toggle-enabled").replace("%name%", matchedService.name()));
      } else {
        source.sendMessage(
          I18n.trans("command-service-toggle-disabled").replace("%name%", matchedService.name()));
      }
    }
  }

  @CommandMethod("service|ser <name> includeInclusions")
  public void includeInclusions(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceInclusions();
    }
  }

  @CommandMethod("service|ser <name> includeTemplates")
  public void includeTemplates(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceTemplates();
    }
  }

  @CommandMethod("service|ser <name> deployResources")
  public void deployResources(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (var matchedService : matchedServices) {
      matchedService.provider().removeAndExecuteDeployments();
    }
  }

  @CommandMethod("service|ser <name> command|cmd <command>")
  public void sendCommand(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Greedy @Argument("command") String command
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().runCommand(command);
    }
  }

  @CommandMethod("service|ser <name> add deployment <deployment>")
  public void addDeployment(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Argument("deployment") ServiceTemplate template
  ) {
    var deployment = ServiceDeployment.builder().template(template).build();
    for (var matchedService : matchedServices) {
      matchedService.provider().addServiceDeployment(deployment);
    }
  }

  @CommandMethod("service|ser <name> add template <template>")
  public void addTemplate(
    CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Argument("template") ServiceTemplate template
  ) {
    for (var matchedService : matchedServices) {
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
    var remoteInclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();
    for (var matchedService : matchedServices) {
      matchedService.provider().addServiceRemoteInclusion(remoteInclusion);
    }
  }

  @EventListener(channel = "service:screen")
  public void handleLogEntry(CloudServiceLogEntryEvent event) {
    LOGGER.info(String.format("&b[%s] %s", event.serviceInfo().name(), event.line()));
  }

  private void displayServiceInfo(CommandSource source, @Nullable ServiceInfoSnapshot service,
    boolean showCustomProperties) {
    if (service == null) {
      return;
    }

    Collection<String> list = new ArrayList<>(Arrays.asList(
      " ",
      "* CloudService: " + service.serviceId().uniqueId().toString(),
      "* Name: " + service.serviceId().name(),
      "* Node: " + service.serviceId().nodeUniqueId(),
      "* Address: " + service.address().host() + ":" + service.address().port()
    ));

    if (!service.address().host().equals(service.connectAddress().host())) {
      list.add("* Address for connections: " + service.connectAddress().host() + ":" + service
        .connectAddress().port());
    }

    if (service.connected()) {
      list.add("* Connected: " + DATE_FORMAT.format(service.connectedTime()));
    } else {
      list.add("* Connected: false");
    }

    list.add("* Lifecycle: " + service.lifeCycle());
    list.add("* Groups: " + String.join(", ", service.configuration().groups()));

    if (!service.configuration().includes().isEmpty()) {
      list.add(" ");
      list.add("* Includes:");

      for (var inclusion : service.configuration().includes()) {
        list.add("- " + inclusion.url() + " => " + inclusion.destination());
      }
    }

    if (!service.configuration().templates().isEmpty()) {
      list.add(" ");
      list.add("* Templates:");

      for (var template : service.configuration().templates()) {
        list.add("- " + template);
      }
    }

    if (!service.configuration().deployments().isEmpty()) {
      list.add(" ");
      list.add("* Deployments:");

      for (var deployment : service.configuration().deployments()) {
        list.add("- ");
        list
          .add(
            "Template:  " + deployment.template());
        list.add("Excludes: " + deployment.excludes());
      }
    }

    list.add(" ");
    list.add("* ServiceInfoSnapshot | " + DATE_FORMAT.format(service.creationTime()));

    list.addAll(Arrays.asList(
      "PID: " + service.processSnapshot().pid(),
      "CPU usage: " + CPUUsageResolver.FORMAT
        .format(service.processSnapshot().cpuUsage()) + "%",
      "Threads: " + service.processSnapshot().threads().size(),
      "Heap usage: " + (service.processSnapshot().heapUsageMemory() / 1048576) + "/" +
        (service.processSnapshot().maxHeapMemory() / 1048576) + "MB",
      " "
    ));

    if (showCustomProperties) {
      list.add("Properties:");
      list.addAll(Arrays.asList(service.properties().toPrettyJson().split("\n")));
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
