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
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.annotation.CommandAlias;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.exception.ArgumentNotAvailableException;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.INameable;
import de.dytanic.cloudnet.common.WildcardUtil;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

@CommandAlias("ser")
@CommandPermission("cloudnet.command.service")
@Description("Manages all services in the cluster")
public class CommandService {

  public static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("([\\w+-]+)-(\\d+)");
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  @Parser(name = "single", suggestions = "service")
  public ServiceInfoSnapshot singleServiceParser(CommandContext<CommandSource> $, Queue<String> input) {
    String name = input.remove();
    ServiceInfoSnapshot serviceInfoSnapshot = CloudNet.getInstance().getCloudServiceProvider()
      .getCloudServiceByName(name);
    if (serviceInfoSnapshot == null) {
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-service-service-not-found"));
    }
    return serviceInfoSnapshot;
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
      throw new ArgumentNotAvailableException(LanguageManager.getMessage("command-service-service-not-found"));
    }

    return matchedServices;
  }

  @Parser(name = "staticWildcard")
  public Collection<ServiceInfoSnapshot> staticWildcardServiceParser(CommandContext<CommandSource> $,
    Queue<String> input) {
    String name = input.remove();
    Collection<ServiceInfoSnapshot> knownServices = CloudNet.getInstance().getCloudServiceProvider().getCloudServices();
    return WildcardUtil.filterWildcard(knownServices, name);
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

    //TODO: think about this
    /*ServiceListCommandEvent event = CloudNet.getInstance().getEventManager()
      .callEvent(new ServiceListCommandEvent(services));*/
    for (ServiceInfoSnapshot serviceInfoSnapshot : services) {
      /*String extension = event.getAdditionalParameters()
        .stream()
        .map(function -> function.apply(serviceInfoSnapshot))
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" | "));
      if (!extension.isEmpty()) {
        extension = " | " + extension;
      }*/

      if (useNamesOnly) {
        source.sendMessage(
          serviceInfoSnapshot.getServiceId().getName() + " | " + serviceInfoSnapshot.getServiceId().getUniqueId());
      } else {
        source.sendMessage(
          "Name: " + serviceInfoSnapshot.getServiceId().getName() + " | Lifecycle: "
            + serviceInfoSnapshot.getLifeCycle() +
            " | " + (serviceInfoSnapshot.isConnected()
            ? "Connected" : "Not Connected") //+ extension
        );
      }
    }
    StringBuilder builder = new StringBuilder(
      String.format("=> Showing %d service(s)", services.size()));
   /* for (String parameter : event.getAdditionalSummary()) {
      builder.append("; ").append(parameter);
    }*/
    source.sendMessage(builder.toString());
  }

  @CommandMethod("service|ser <name>")
  public void displayBasicServiceInfo(CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices, @Flag("full") boolean customProperties) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      ServiceInfoSnapshot updatedService = matchedService.provider().forceUpdateServiceInfo();
      this.displayServiceInfo(source, updatedService, customProperties);
    }
  }

  @CommandMethod("service|ser <name> start")
  public void startServices(CommandSource source,
    @Argument("name") String serviceName) {

    Collection<ServiceInfoSnapshot> matchedServices = this.staticWildcardServiceParser(
      null, new ConcurrentLinkedQueue<>(Collections.singletonList(serviceName)));
    // there may be a static service with that name, but as it can be started even if it's not prepared we need to check
    if (matchedServices.isEmpty()) {
      Matcher nameMatcher = SERVICE_NAME_PATTERN.matcher(serviceName);
      String taskName = nameMatcher.group(1);
      Integer id = Ints.tryParse(nameMatcher.group(2));

      if (id != null) {
        ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider().getServiceTask(taskName);
        if (serviceTask != null) {
          ServiceInfoSnapshot service = ServiceConfiguration.builder(serviceTask)
            .taskId(id)
            .build().createNewService();

          if (service != null) {
            matchedServices.add(service);
          }
        }
      }
    }
    if (matchedServices.isEmpty()) {
      source.sendMessage("No services found");
    }

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

  @CommandMethod("service|ser <name> delete|del")
  public void deleteServices(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().delete();
    }
  }

  @CommandMethod("service|ser <name> includeInclusions")
  public void includeInclusions(CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().includeWaitingServiceInclusions();
    }
  }

  @CommandMethod("service|ser <name> includeTemplates")
  public void includeTemplates(CommandSource source,
    @Argument("name") Collection<ServiceInfoSnapshot> matchedServices) {
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

  @CommandMethod("service|ser <name> command <command>")
  public void sendCommand(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Greedy @Argument("command") String command) {
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().runCommand(command);
    }
  }

  @CommandMethod("service|ser <name> add deployment <deployment>")
  public void addDeployment(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Argument("deployment") ServiceTemplate template) {
    ServiceDeployment deployment = new ServiceDeployment(template, new ArrayList<>());
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().addServiceDeployment(deployment);
    }
  }

  @CommandMethod("service|ser <name> add template <template>")
  public void addTemplate(CommandSource source, @Argument("name") Collection<ServiceInfoSnapshot> matchedServices,
    @Argument("template") ServiceTemplate template) {
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
    ServiceRemoteInclusion remoteInclusion = new ServiceRemoteInclusion(url, path);
    for (ServiceInfoSnapshot matchedService : matchedServices) {
      matchedService.provider().addServiceRemoteInclusion(remoteInclusion);
    }
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
    list.add(" ");

    if (!service.getConfiguration().getIncludes().isEmpty()) {
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
      "CPU usage: " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT
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

}
