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

package de.dytanic.cloudnet.command.commands;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.collection;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.template;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.url;

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.WildcardUtil;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.event.ServiceListCommandEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandService extends SubCommandHandler {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  public CommandService() {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            Collection<ServiceInfoSnapshot> targetServiceInfoSnapshots = CloudNetDriver.getInstance()
              .getCloudServiceProvider().getCloudServices().stream()
              .filter(serviceInfoSnapshot -> !properties.containsKey("id")
                || serviceInfoSnapshot.getServiceId().getUniqueId().toString().toLowerCase()
                .contains(properties.get("id").toLowerCase()))
              .filter(serviceInfoSnapshot -> !properties.containsKey("group")
                || Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(properties.get("group")))
              .filter(serviceInfoSnapshot -> !properties.containsKey("task")
                || properties.get("task").toLowerCase()
                .contains(serviceInfoSnapshot.getServiceId().getTaskName().toLowerCase()))
              .sorted()
              .collect(Collectors.toList());

            ServiceListCommandEvent event = CloudNet.getInstance().getEventManager()
              .callEvent(new ServiceListCommandEvent(targetServiceInfoSnapshots));
            for (ServiceInfoSnapshot serviceInfoSnapshot : targetServiceInfoSnapshots) {
              String extension = event.getAdditionalParameters().stream()
                .map(function -> function.apply(serviceInfoSnapshot))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" | "));
              if (!extension.isEmpty()) {
                extension = " | " + extension;
              }

              if (!properties.containsKey("names")) {
                sender.sendMessage(
                  serviceInfoSnapshot.getServiceId().getUniqueId().toString().split("-")[0] +
                    " | Name: " + serviceInfoSnapshot.getServiceId().getName() +
                    " | Node: " + serviceInfoSnapshot.getServiceId().getNodeUniqueId() +
                    " | Status: " + serviceInfoSnapshot.getLifeCycle() +
                    " | Address: " + serviceInfoSnapshot.getAddress().getHost() + ":" +
                    serviceInfoSnapshot.getAddress().getPort() +
                    " | " + (serviceInfoSnapshot.isConnected() ? "Connected" : "Not Connected") +
                    extension
                );
              } else {
                sender.sendMessage(
                  serviceInfoSnapshot.getServiceId().getTaskName() + "-" + serviceInfoSnapshot.getServiceId()
                    .getTaskServiceId() +
                    " | " + serviceInfoSnapshot.getServiceId().getUniqueId() + extension);
              }
            }

            StringBuilder builder = new StringBuilder(
              String.format("=> Showing %d service(s)", targetServiceInfoSnapshots.size()));
            for (String parameter : event.getAdditionalSummary()) {
              builder.append("; ").append(parameter);
            }
            sender.sendMessage(builder.toString());
          },
          subCommand -> subCommand.enableProperties().appendUsage("| id=<text> | task=<text> | group=<text> | --names"),
          anyStringIgnoreCase("list", "l")
        )

        .prefix(dynamicString(
          "name",
          LanguageManager.getMessage("command-service-service-not-found"),
          input -> {
            if (WildcardUtil
              .anyMatch(CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(), input, false)) {
              return true;
            }
            String[] splitName = input.split("-");
            return splitName.length == 2 &&
              CloudNetDriver.getInstance().getServiceTaskProvider().isServiceTaskPresent(splitName[0]) &&
              Ints.tryParse(splitName[1]) != null;
          },
          () -> {
            Collection<String> values = CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices()
              .stream()
              .map(ServiceInfoSnapshot::getName)
              .collect(Collectors.toList());
            values.addAll(CloudNetDriver.getInstance().getServiceTaskProvider().getPermanentServiceTasks().stream()
              .filter(serviceTask ->
                CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(serviceTask.getName()).size()
                  > 1)
              .map(serviceTask -> serviceTask.getName() + "-*")
              .collect(Collectors.toList()));
            return values;
          }
        ))
        .preExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          String name = (String) args.argument("name").get();
          Collection<ServiceInfoSnapshot> serviceInfoSnapshots = WildcardUtil.filterWildcard(
            CloudNetDriver.getInstance().getCloudServiceProvider().getCloudServices(),
            name,
            false
          );
          if (serviceInfoSnapshots.isEmpty() && "start".equalsIgnoreCase(String.valueOf(args.argument(1)))) {
            String[] splitName = name.split("-");
            String taskName = splitName[0];
            Integer id = Ints.tryParse(splitName[1]);

            if (id != null) {
              ServiceTask task = CloudNetDriver.getInstance().getServiceTaskProvider().getServiceTask(taskName);
              if (task != null) {
                ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceFactory()
                  .createCloudService(task, id);
                if (serviceInfoSnapshot != null) {
                  serviceInfoSnapshots.add(serviceInfoSnapshot);
                }
              }
            }
          }
          internalProperties.put("services", serviceInfoSnapshots);
        })

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> {
              ServiceInfoSnapshot currentServiceInfoSnapshot = serviceInfoSnapshot.provider().forceUpdateServiceInfo();
              display(sender, currentServiceInfoSnapshot == null ? serviceInfoSnapshot : currentServiceInfoSnapshot,
                false);
            }))
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> {
              ServiceInfoSnapshot currentServiceInfoSnapshot = serviceInfoSnapshot.provider().forceUpdateServiceInfo();
              display(sender, currentServiceInfoSnapshot == null ? serviceInfoSnapshot : currentServiceInfoSnapshot,
                true);
            }),
          anyStringIgnoreCase("info", "i")
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().start()),
          exactStringIgnoreCase("start")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            if (properties.containsKey("--force")) {
              forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().kill());
            } else {
              forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().stop());
            }
          },
          subCommand -> subCommand.enableProperties().appendUsage("| --force"),
          anyStringIgnoreCase("stop", "shutdown")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().delete()),
          anyStringIgnoreCase("delete", "del")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties,
            serviceInfoSnapshot -> serviceInfoSnapshot.provider().includeWaitingServiceInclusions()),
          exactStringIgnoreCase("includeInclusions")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().includeWaitingServiceTemplates()),
          exactStringIgnoreCase("includeTemplates")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().deployResources()),
          exactStringIgnoreCase("deployResources")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider().restart()),
          exactStringIgnoreCase("restart")
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) ->
            forEachService(internalProperties, serviceInfoSnapshot -> serviceInfoSnapshot.provider()
              .runCommand((String) args.argument("command").get())),
          subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length).setMaxArgs(Integer.MAX_VALUE),
          anyStringIgnoreCase("command", "cmd"),
          dynamicString("command")
        )

        .prefix(exactStringIgnoreCase("add"))
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> {
              ServiceTemplate template = (ServiceTemplate) args.argument("storage:prefix/name").get();
              Collection<String> excludes = (Collection<String>) args.argument("excludedFiles separated by \";\"")
                .orElse(new ArrayList<>());

              serviceInfoSnapshot.provider().addServiceDeployment(new ServiceDeployment(template, excludes));

              sender.sendMessage(LanguageManager.getMessage("command-service-add-deployment-success"));
            }),
          subCommand -> subCommand.setMinArgs(subCommand.getRequiredArguments().length - 1)
            .setMaxArgs(Integer.MAX_VALUE),
          exactStringIgnoreCase("deployment"),
          template("storage:prefix/name"),
          collection("excludedFiles separated by \";\"")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> {
              ServiceTemplate template = (ServiceTemplate) args.argument("storage:prefix/name").get();

              serviceInfoSnapshot.provider().addServiceTemplate(template);

              sender.sendMessage(LanguageManager.getMessage("command-service-add-template-success"));
            }),
          exactStringIgnoreCase("template"),
          template("storage:prefix/name")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> forEachService(
            internalProperties, serviceInfoSnapshot -> {
              String url = (String) args.argument("url").get();
              String target = (String) args.argument("targetPath").get();

              serviceInfoSnapshot.provider().addServiceRemoteInclusion(new ServiceRemoteInclusion(url, target));

              sender.sendMessage(LanguageManager.getMessage("command-service-add-inclusion-success"));
            }),
          exactStringIgnoreCase("inclusion"),
          url("url"),
          dynamicString("targetPath")
        )

        .getSubCommands(),
      "service", "ser"
    );
    super.prefix = "cloudnet";
    super.permission = "cloudnet.command." + super.names[0];
    super.description = LanguageManager.getMessage("command-description-service");
  }


  private static void forEachService(Map<String, Object> internalProperties, Consumer<ServiceInfoSnapshot> consumer) {
    for (Object serviceInfoSnapshot : ((Collection<?>) internalProperties.get("services"))) {
      if (serviceInfoSnapshot instanceof ServiceInfoSnapshot) {
        consumer.accept((ServiceInfoSnapshot) serviceInfoSnapshot);
      }
    }
  }

  private static void display(ICommandSender sender, ServiceInfoSnapshot serviceInfoSnapshot, boolean full) {

    Collection<String> list = new ArrayList<>(Arrays.asList(
      " ",
      "* CloudService: " + serviceInfoSnapshot.getServiceId().getUniqueId().toString(),
      "* Name: " + serviceInfoSnapshot.getServiceId().getTaskName() + "-" + serviceInfoSnapshot.getServiceId()
        .getTaskServiceId(),
      "* Node: " + serviceInfoSnapshot.getServiceId().getNodeUniqueId(),
      "* Address: " + serviceInfoSnapshot.getAddress().getHost() + ":" + serviceInfoSnapshot.getAddress().getPort()
    ));

    if (serviceInfoSnapshot.getServiceId().getEnvironment().isMinecraftServer()
      && !serviceInfoSnapshot.getAddress().getHost().equals(serviceInfoSnapshot.getConnectAddress().getHost())) {
      list.add(
        "* Address for connections: " + serviceInfoSnapshot.getConnectAddress().getHost() + ":" + serviceInfoSnapshot
          .getConnectAddress().getPort());
    }

    if (serviceInfoSnapshot.isConnected()) {
      list.add("* Connected: " + DATE_FORMAT.format(serviceInfoSnapshot.getConnectedTime()));
    } else {
      list.add("* Connected: false");
    }

    list.addAll(Arrays.asList(
      "* Lifecycle: " + serviceInfoSnapshot.getLifeCycle(),
      "* Groups: " + Arrays.toString(serviceInfoSnapshot.getConfiguration().getGroups()),
      " "
    ));

    list.add("* Includes:");

    for (ServiceRemoteInclusion inclusion : serviceInfoSnapshot.getConfiguration().getIncludes()) {
      list.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
    }

    list.add(" ");
    list.add("* Templates:");

    for (ServiceTemplate template : serviceInfoSnapshot.getConfiguration().getTemplates()) {
      list.add("- " + template.getStorage() + ":" + template.getTemplatePath());
    }

    list.add(" ");
    list.add("* Deployments:");

    for (ServiceDeployment deployment : serviceInfoSnapshot.getConfiguration().getDeployments()) {
      list.add("- ");
      list
        .add("Template:  " + deployment.getTemplate().getStorage() + ":" + deployment.getTemplate().getTemplatePath());
      list.add("Excludes: " + deployment.getExcludes());
    }

    list.add(" ");
    list.add("* ServiceInfoSnapshot | " + DATE_FORMAT.format(serviceInfoSnapshot.getCreationTime()));

    list.addAll(Arrays.asList(
      "PID: " + serviceInfoSnapshot.getProcessSnapshot().getPid(),
      "CPU usage: " + CPUUsageResolver.CPU_USAGE_OUTPUT_FORMAT
        .format(serviceInfoSnapshot.getProcessSnapshot().getCpuUsage()) + "%",
      "Threads: " + serviceInfoSnapshot.getProcessSnapshot().getThreads().size(),
      "Heap usage: " + (serviceInfoSnapshot.getProcessSnapshot().getHeapUsageMemory() / 1048576) + "/" +
        (serviceInfoSnapshot.getProcessSnapshot().getMaxHeapMemory() / 1048576) + "MB",
      "Loaded classes: " + serviceInfoSnapshot.getProcessSnapshot().getCurrentLoadedClassCount(),
      "Unloaded classes: " + serviceInfoSnapshot.getProcessSnapshot().getUnloadedClassCount(),
      "Total loaded classes: " + serviceInfoSnapshot.getProcessSnapshot().getTotalLoadedClassCount(),
      " "
    ));

    if (full) {
      list.add("Properties:");
      list.addAll(Arrays.asList(serviceInfoSnapshot.getProperties().toPrettyJson().split("\n")));
      list.add(" ");
    }

    sender.sendMessage(list.toArray(new String[0]));
  }

}
