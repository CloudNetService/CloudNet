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

package eu.cloudnetservice.node.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.specifier.Quoted;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.base.Splitter;
import eu.cloudnetservice.common.Nameable;
import eu.cloudnetservice.common.WildcardUtil;
import eu.cloudnetservice.common.collection.Pair;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.unsafe.CPUUsageResolver;
import eu.cloudnetservice.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLogEntryEvent;
import eu.cloudnetservice.driver.provider.SpecificCloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.Node;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("ser")
@CommandPermission("cloudnet.command.service")
@Description("command-service-description")
public final class ServiceCommand {

  private static final Logger LOGGER = LogManager.logger(ServiceCommand.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
  private static final Splitter SEMICOLON_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();

  // there are different ways to display the services
  private static final RowBasedFormatter<ServiceInfoSnapshot> NAMES_ONLY = RowBasedFormatter.<ServiceInfoSnapshot>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "UID").build())
    .column(ServiceInfoSnapshot::name)
    .column(service -> service.serviceId().uniqueId())
    .build();
  private static final RowBasedFormatter<ServiceInfoSnapshot> SERVICES = RowBasedFormatter.<ServiceInfoSnapshot>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "Lifecycle", "Address", "Node", "State").build())
    .column(ServiceInfoSnapshot::name)
    .column(ServiceInfoSnapshot::lifeCycle)
    .column(ServiceInfoSnapshot::address)
    .column(service -> service.serviceId().nodeUniqueId())
    .column(service -> service.connected() ? "Connected" : "Not connected")
    .build();

  public ServiceCommand() {
    Node.instance().eventManager().registerListener(this);
  }

  public static @NonNull Collection<Pattern> parseDeploymentPatterns(@Nullable String input, boolean caseSensitive) {
    return input == null ? Set.of() : SEMICOLON_SPLITTER.splitToStream(input)
      .map(pattern -> WildcardUtil.fixPattern(pattern, caseSensitive))
      .filter(Objects::nonNull)
      .toList();
  }

  @Suggestions("service")
  public @NonNull List<String> suggestService(@NonNull CommandContext<?> $, @NonNull String input) {
    return Node.instance().cloudServiceProvider().services()
      .stream()
      .map(Nameable::name)
      .toList();
  }

  @Parser(suggestions = "service")
  public @NonNull Collection<ServiceInfoSnapshot> wildcardServiceParser(
    @NonNull CommandContext<?> $,
    @NonNull Queue<String> input
  ) {
    var name = input.remove();
    var knownServices = Node.instance().cloudServiceProvider().services();
    var matchedServices = WildcardUtil.filterWildcard(knownServices, name);
    if (matchedServices.isEmpty()) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-service-not-found"));
    }

    return matchedServices;
  }

  @CommandMethod("service|ser list|l")
  public void displayServices(
    @NonNull CommandSource source,
    @Nullable @Flag("id") Integer id,
    @Nullable @Flag("task") String taskName,
    @Nullable @Flag("group") String groupName,
    @Flag("names") boolean useNamesOnly
  ) {
    Collection<ServiceInfoSnapshot> services = Node.instance().cloudServiceProvider().services()
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
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Flag("full") boolean customProperties
  ) {
    for (var matchedService : matchedServices) {
      var updatedService = matchedService.provider().forceUpdateServiceInfo();
      this.displayServiceInfo(source, updatedService, customProperties);
    }
  }

  @CommandMethod("service|ser <name> start")
  public void startServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().start();
    }
  }

  @CommandMethod("service|ser <name> restart")
  public void restartServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().restart();
    }
  }

  @CommandMethod("service|ser <name> stop")
  public void stopServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().stop();
    }
  }

  @CommandMethod("service|ser <name> copy|cp")
  public void copyService(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "name") Collection<ServiceInfoSnapshot> services,
    @Nullable @Flag("template") ServiceTemplate template,
    @Nullable @Flag("excludes") @Quoted String excludes,
    @Nullable @Flag("includes") @Quoted String includes,
    @Flag("case-sensitive") boolean caseSensitive
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
      source.sendMessage(I18n.trans("command-service-copy-no-default-template"));
      return;
    }
    // split on a semicolon and try to fix the patterns the user entered
    var parsedExcludes = parseDeploymentPatterns(excludes, caseSensitive);
    var parsedIncludes = parseDeploymentPatterns(includes, caseSensitive);
    for (var target : targets) {
      target.first().addServiceDeployment(ServiceDeployment.builder()
        .template(target.second())
        .excludes(parsedExcludes)
        .includes(parsedIncludes)
        .withDefaultExclusions()
        .build());
      target.first().removeAndExecuteDeployments();
      // send a message for each service we did copy the template of
      //noinspection ConstantConditions
      source.sendMessage(I18n.trans("command-service-copy-success",
        target.first().serviceInfo().name(),
        target.second().toString()));
    }
  }

  @CommandMethod("service|ser <name> delete|del")
  public void deleteServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().delete();
    }
  }

  @CommandMethod(value = "service|ser <name> toggle", requiredSender = ConsoleCommandSource.class)
  public void toggleScreens(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      if (matchedService.provider().toggleScreenEvents(ChannelMessageSender.self(), "service:screen")) {
        for (var cachedLogMessage : matchedService.provider().cachedLogMessages()) {
          LOGGER.info(String.format("&b[%s] %s", matchedService.name(), cachedLogMessage));
        }
        source.sendMessage(I18n.trans("command-service-toggle-enabled", matchedService.name()));
      } else {
        source.sendMessage(I18n.trans("command-service-toggle-disabled", matchedService.name()));
      }
    }
  }

  @CommandMethod("service|ser <name> includeInclusions")
  public void includeInclusions(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceInclusions();
    }
    source.sendMessage(I18n.trans("command-service-include-inclusion-success"));
  }

  @CommandMethod("service|ser <name> includeTemplates")
  public void includeTemplates(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceTemplates();
    }
    source.sendMessage(I18n.trans("command-service-include-templates-success"));
  }

  @CommandMethod("service|ser <name> deployResources")
  public void deployResources(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().removeAndExecuteDeployments();
    }
    source.sendMessage(I18n.trans("command-service-deploy-deployment-success"));
  }

  @CommandMethod("service|ser <name> command|cmd <command>")
  public void sendCommand(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @NonNull @Greedy @Argument("command") String command
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().runCommand(command);
    }
  }

  @CommandMethod("service|ser <name> add deployment <deployment>")
  public void addDeployment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @NonNull @Argument("deployment") ServiceTemplate template
  ) {
    var deployment = ServiceDeployment.builder().template(template).build();
    for (var matchedService : matchedServices) {
      matchedService.provider().addServiceDeployment(deployment);
    }
    source.sendMessage(I18n.trans("command-service-add-deployment-success", deployment.template().fullName()));
  }

  @CommandMethod("service|ser <name> add template <template>")
  public void addTemplate(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @NonNull @Argument("template") ServiceTemplate template
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().addServiceTemplate(template);
    }
    source.sendMessage(I18n.trans("command-service-add-template-success", template.fullName()));
  }

  @CommandMethod("service|ser <name> add inclusion <url> <path>")
  public void addInclusion(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @NonNull @Argument("url") String url,
    @NonNull @Argument("path") String path
  ) {
    var remoteInclusion = ServiceRemoteInclusion.builder().url(url).destination(path).build();
    for (var matchedService : matchedServices) {
      matchedService.provider().addServiceRemoteInclusion(remoteInclusion);
    }
    source.sendMessage(I18n.trans("command-service-add-inclusion-success", remoteInclusion.toString()));
  }

  @EventListener(channel = "service:screen")
  public void handleLogEntry(@NonNull CloudServiceLogEntryEvent event) {
    LOGGER.info(String.format("&b[%s] %s", event.serviceInfo().name(), event.line()));
  }

  private void displayServiceInfo(
    @NonNull CommandSource source,
    @Nullable ServiceInfoSnapshot service,
    boolean showCustomProperties
  ) {
    if (service == null) {
      return;
    }

    Collection<String> list = new ArrayList<>(List.of(
      " ",
      "* CloudService: " + service.serviceId().uniqueId(),
      "* Name: " + service.serviceId().name(),
      "* Node: " + service.serviceId().nodeUniqueId(),
      "* Address: " + service.address().host() + ":" + service.address().port()
    ));

    if (service.connected()) {
      list.add("* Connected: " + DATE_FORMAT.format(service.connectedTime()));
    } else {
      list.add("* Connected: false");
    }

    list.add("* Lifecycle: " + service.lifeCycle());
    list.add("* Groups: " + String.join(", ", service.configuration().groups()));

    if (!service.configuration().inclusions().isEmpty()) {
      list.add(" ");
      list.add("* Includes:");

      for (var inclusion : service.configuration().inclusions()) {
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
        list.add("Template:  " + deployment.template());
        list.add("Excludes: " + deployment.excludes());
      }
    }

    list.add(" ");
    list.add("* ServiceInfoSnapshot | " + DATE_FORMAT.format(service.creationTime()));

    list.addAll(List.of(
      "PID: " + service.processSnapshot().pid(),
      "CPU usage: " + CPUUsageResolver.FORMAT.format(service.processSnapshot().cpuUsage()) + "%",
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
}
