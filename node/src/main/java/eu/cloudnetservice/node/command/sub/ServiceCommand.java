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

package eu.cloudnetservice.node.command.sub;

import com.google.common.base.Splitter;
import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.resource.ResourceFormatter;
import eu.cloudnetservice.common.util.WildcardUtil;
import eu.cloudnetservice.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.event.events.service.CloudServiceLogEntryEvent;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceDeployment;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.driver.service.ServiceTemplate;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.command.source.ConsoleCommandSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.NonNull;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@CommandAlias("ser")
@Permission("cloudnet.command.service")
@Description("command-service-description")
public final class ServiceCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCommand.class);
  private static final Splitter SEMICOLON_SPLITTER = Splitter.on(';').omitEmptyStrings().trimResults();
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  // there are different ways to display the services
  private static final RowedFormatter<ServiceInfoSnapshot> NAMES_ONLY = RowedFormatter.<ServiceInfoSnapshot>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "UID").build())
    .column(ServiceInfoSnapshot::name)
    .column(service -> service.serviceId().uniqueId())
    .build();
  private static final RowedFormatter<ServiceInfoSnapshot> SERVICES = RowedFormatter.<ServiceInfoSnapshot>builder()
    .defaultFormatter(ColumnFormatter.builder().columnTitles("Name", "Lifecycle", "Address", "Node", "State").build())
    .column(ServiceInfoSnapshot::name)
    .column(ServiceInfoSnapshot::lifeCycle)
    .column(ServiceInfoSnapshot::address)
    .column(service -> service.serviceId().nodeUniqueId())
    .column(service -> service.connected() ? "Connected" : "Not connected")
    .build();

  private final CloudServiceProvider cloudServiceProvider;

  @Inject
  public ServiceCommand(@NonNull EventManager eventManager, @NonNull CloudServiceProvider cloudServiceProvider) {
    this.cloudServiceProvider = cloudServiceProvider;
    eventManager.registerListener(this);
  }

  public static @NonNull Collection<Pattern> parseDeploymentPatterns(@Nullable String input, boolean caseSensitive) {
    return input == null ? Set.of() : SEMICOLON_SPLITTER.splitToStream(input)
      .map(pattern -> WildcardUtil.fixPattern(pattern, caseSensitive))
      .filter(Objects::nonNull)
      .toList();
  }

  @Suggestions("service")
  public @NonNull Stream<String> suggestService() {
    return this.cloudServiceProvider.services()
      .stream()
      .map(Named::name);
  }

  @Parser(suggestions = "service")
  public @NonNull Collection<ServiceInfoSnapshot> wildcardServiceParser(@NonNull CommandInput input) {
    var name = input.readString();
    var knownServices = this.cloudServiceProvider.services();
    var matchedServices = WildcardUtil.filterWildcard(knownServices, name);
    if (matchedServices.isEmpty()) {
      throw new ArgumentNotAvailableException(I18n.trans("command-service-service-not-found"));
    }

    return matchedServices;
  }

  @Command("service|ser list|l")
  public void displayServices(
    @NonNull CommandSource source,
    @Nullable @Flag("id") Integer id,
    @Nullable @Flag("task") String taskName,
    @Nullable @Flag("group") String groupName,
    @Flag("names") boolean useNamesOnly
  ) {
    Collection<ServiceInfoSnapshot> services = this.cloudServiceProvider.services()
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

  @Command("service|ser <name>")
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

  @Command("service|ser <name> start")
  public void startServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().start();
    }
  }

  @Command("service|ser <name> restart")
  public void restartServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().restart();
    }
  }

  @Command("service|ser <name> stop")
  public void stopServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().stop();
    }
  }

  @Command("service|ser <name> copy|cp")
  public void copyService(
    @NonNull CommandSource source,
    @NonNull @Argument(value = "name") Collection<ServiceInfoSnapshot> services,
    @Nullable @Flag("template") ServiceTemplate template,
    @Nullable @Flag("excludes") @Quoted String excludes,
    @Nullable @Flag("includes") @Quoted String includes,
    @Flag("case-sensitive") boolean caseSensitive
  ) {
    var service = services.iterator().next();
    var serviceProvider = service.provider();
    if (template == null) {
      template = serviceProvider.installedTemplates().stream()
        .filter(st -> st.prefix().equalsIgnoreCase(service.serviceId().taskName()))
        .filter(st -> st.name().equalsIgnoreCase("default"))
        .findFirst()
        .orElse(null);

      if (template == null) {
        source.sendMessage(I18n.trans("command-service-copy-no-default-template", service.serviceId().name()));
        return;
      }
    }

    // split on a semicolon and try to fix the patterns the user entered
    var parsedExcludes = parseDeploymentPatterns(excludes, caseSensitive);
    var parsedIncludes = parseDeploymentPatterns(includes, caseSensitive);
    serviceProvider.addServiceDeployment(ServiceDeployment.builder()
      .template(template)
      .excludes(parsedExcludes)
      .includes(parsedIncludes)
      .withDefaultExclusions()
      .build());
    serviceProvider.removeAndExecuteDeployments();
    source.sendMessage(I18n.trans("command-service-copy-success", service.serviceId().name(), template));
  }

  @Command("service|ser <name> delete|del")
  public void deleteServices(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().delete();
    }
  }

  @Command(value = "service|ser <name> screen|toggle", requiredSender = ConsoleCommandSource.class)
  public void toggleScreens(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      if (matchedService.provider().toggleScreenEvents(ChannelMessageSender.self(), "service:screen")) {
        for (var cachedLogMessage : matchedService.provider().cachedLogMessages()) {
          LOGGER.info("&b[{}] {}", matchedService.name(), cachedLogMessage);
        }
        source.sendMessage(I18n.trans("command-service-toggle-enabled", matchedService.name()));
      } else {
        source.sendMessage(I18n.trans("command-service-toggle-disabled", matchedService.name()));
      }
    }
  }

  @Command("service|ser <name> includeInclusions")
  public void includeInclusions(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceInclusions();
    }
    source.sendMessage(I18n.trans("command-service-include-inclusion-success"));
  }

  @Command("service|ser <name> includeTemplates")
  public void includeTemplates(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceTemplates();
    }
    source.sendMessage(I18n.trans("command-service-include-templates-success"));
  }

  @Command("service|ser <name> deployResources")
  public void deployResources(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().removeAndExecuteDeployments();
    }
    source.sendMessage(I18n.trans("command-service-deploy-deployment-success"));
  }

  @Command("service|ser <name> command|cmd <command>")
  public void sendCommand(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @NonNull @Greedy @Argument("command") String command
  ) {
    for (var matchedService : matchedServices) {
      matchedService.provider().runCommand(command);
    }
  }

  @Command("service|ser <name> add deployment <deployment>")
  public void addDeployment(
    @NonNull CommandSource source,
    @NonNull @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @NonNull @Argument("deployment") ServiceTemplate template,
    @Nullable @Flag("excludes") @Quoted String excludes,
    @Nullable @Flag("includes") @Quoted String includes,
    @Flag("case-sensitive") boolean caseSensitive
  ) {
    var deployment = ServiceDeployment.builder()
      .template(template)
      .excludes(ServiceCommand.parseDeploymentPatterns(excludes, caseSensitive))
      .includes(ServiceCommand.parseDeploymentPatterns(includes, caseSensitive))
      .withDefaultExclusions()
      .build();
    for (var matchedService : matchedServices) {
      matchedService.provider().addServiceDeployment(deployment);
    }
    source.sendMessage(I18n.trans("command-service-add-deployment-success", deployment.template().fullName()));
  }

  @Command("service|ser <name> add template <template>")
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

  @Command("service|ser <name> add inclusion <url> <path>")
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
    LOGGER.info("&b[{}] {}", event.serviceInfo().name(), event.line());
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
      var connectTime = Instant.ofEpochMilli(service.connectedTime()).atZone(ZoneId.systemDefault());
      list.add("* Connected: " + DATE_TIME_FORMATTER.format(connectTime));
    } else {
      list.add("* Connected: false");
    }

    list.add("* Lifecycle: " + service.lifeCycle());
    list.add("* Groups: " + String.join(", ", service.configuration().groups()));

    var installedInclusions = service.provider().installedInclusions();
    if (!installedInclusions.isEmpty()) {
      list.add(" ");
      list.add("* Includes:");

      for (var inclusion : installedInclusions) {
        list.add("- " + inclusion.url() + " => " + inclusion.destination());
      }
    }

    var installedTemplates = service.provider().installedTemplates();
    if (!installedTemplates.isEmpty()) {
      list.add(" ");
      list.add("* Templates:");

      for (var template : installedTemplates) {
        list.add("- " + template);
      }
    }

    var installedDeployments = service.provider().installedDeployments();
    if (!installedDeployments.isEmpty()) {
      list.add(" ");
      list.add("* Deployments:");

      for (var deployment : installedDeployments) {
        list.add("- ");
        list.add("Template:  " + deployment.template());
        list.add("Excludes: " + deployment.excludes());
      }
    }

    list.add(" ");

    // service snapshot
    var creationTime = Instant.ofEpochMilli(service.creationTime()).atZone(ZoneId.systemDefault());
    list.add("* ServiceInfoSnapshot | " + DATE_TIME_FORMATTER.format(creationTime));

    list.addAll(List.of(
      "PID: " + service.processSnapshot().pid(),
      "CPU usage: " + ResourceFormatter.formatTwoDigitPrecision(service.processSnapshot().cpuUsage()) + "%",
      "Threads: " + service.processSnapshot().threads().size(),
      "Heap usage: " + (service.processSnapshot().heapUsageMemory() / 1048576) + "/" +
        (service.processSnapshot().maxHeapMemory() / 1048576) + "MB",
      " "
    ));

    if (showCustomProperties) {
      list.add("Properties:");
      list.addAll(Arrays.asList(service.propertyHolder().serializeToString().split("\n")));
      list.add(" ");
    }

    source.sendMessage(list);
  }
}
