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

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactEnum;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.positiveInteger;

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ProcessConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class CommandCreate extends SubCommandHandler {

  public CommandCreate() {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            int count = (int) args.argument("count").orElse(1);
            ServiceTask serviceTask = CloudNet.getInstance().getServiceTaskProvider()
              .getServiceTask((String) args.argument("task").get());
            sender.sendMessage(LanguageManager.getMessage("command-create-start"));
            Collection<ServiceInfoSnapshot> serviceInfoSnapshots = runCloudService(
              properties,
              count,
              serviceTask.getName(),
              serviceTask.getRuntime(),
              serviceTask.isAutoDeleteOnStop(),
              serviceTask.isStaticServices(),
              serviceTask.getAssociatedNodes(),
              serviceTask.getIncludes(),
              serviceTask.getTemplates(),
              serviceTask.getDeployments(),
              serviceTask.getGroups(),
              serviceTask.getDeletedFilesAfterStop(),
              serviceTask.getProcessConfiguration(),
              serviceTask.getStartPort(),
              serviceTask.getJavaCommand()
            );

            if (serviceInfoSnapshots.isEmpty()) {
              sender.sendMessage(LanguageManager.getMessage("command-create-by-task-failed"));
              return;
            }

            listAndStartServices(sender, serviceInfoSnapshots, properties);

            sender.sendMessage(LanguageManager.getMessage("command-create-by-task-success"));
          },
          subCommand -> subCommand.async().enableProperties(),
          exactStringIgnoreCase("by"),
          dynamicString(
            "task",
            LanguageManager.getMessage("command-create-by-task-not-found"),
            name -> CloudNet.getInstance().getServiceTaskProvider().isServiceTaskPresent(name),
            () -> CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()
              .stream()
              .map(ServiceTask::getName)
              .collect(Collectors.toList())
          ),
          positiveInteger("count")
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            String name = (String) args.argument("name").get();
            ServiceEnvironmentType environment = (ServiceEnvironmentType) args.argument(QuestionAnswerTypeEnum.class)
              .get();
            int count = (int) args.argument("count").orElse(1);

            sender.sendMessage(LanguageManager.getMessage("command-create-start"));
            try {
              Collection<ServiceInfoSnapshot> serviceInfoSnapshots = runCloudService(
                properties,
                count,
                name,
                null,
                false,
                false,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ProcessConfiguration(
                  environment,
                  environment.isMinecraftProxy() ? 256 : 512,
                  new ArrayList<>(),
                  new ArrayList<>()
                ),
                environment.getDefaultStartPort(),
                null
              );

              listAndStartServices(sender, serviceInfoSnapshots, properties);

              sender.sendMessage(LanguageManager.getMessage("command-create-new-service-success"));
            } catch (Exception exception) {
              exception.printStackTrace();
            }
          },
          subCommand -> subCommand.async().enableProperties()
            .appendUsage("\n" +
              String.join("\n",
                "parameters: ",
                "- task=<name>",
                "- node=[Node-1;Node-2]",
                "- autoDeleteOnStop=<true : false>",
                "- static=<true : false>",
                "- port=<port>",
                "- memory=<mb>",
                "- groups=[Lobby, Prime, TestLobby]",
                "- runtime=<name>",
                "- javaCommand=<command>",
                "- jvmOptions=[-XX:OptimizeStringConcat;-Xms256M]",
                "- templates=[storage:prefix/name  local:Lobby/Lobby;local:/PremiumLobby]",
                "- deployments=[storage:prefix/name  local:Lobby/Lobby;local:/PremiumLobby]",
                "- --start"
              )
            ),
          exactStringIgnoreCase("new"),
          dynamicString("name"),
          exactEnum(ServiceEnvironmentType.class),
          positiveInteger("count")
        )

        .getSubCommands(),
      "create"
    );

    super.description = LanguageManager.getMessage("command-description-create");
    super.permission = "cloudnet.command.create";
  }

  private static void listAndStartServices(ICommandSender sender, Collection<ServiceInfoSnapshot> serviceInfoSnapshots,
    Properties properties) {
    for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
      if (serviceInfoSnapshot != null) {
        sender.sendMessage(serviceInfoSnapshot.getServiceId().getUniqueId().toString().split("-")[0]
          + " | Name: " + serviceInfoSnapshot.getServiceId().getName()
          + " | Node: " + serviceInfoSnapshot.getServiceId().getNodeUniqueId()
          + " | Address: " + serviceInfoSnapshot.getAddress().getHost() + ":"
          + serviceInfoSnapshot.getAddress().getPort());
      }
    }

    if (properties.containsKey("start")) {
      for (ServiceInfoSnapshot serviceInfoSnapshot : serviceInfoSnapshots) {
        serviceInfoSnapshot.provider().start();
      }
    }
  }

  private static Collection<ServiceInfoSnapshot> runCloudService(
    Properties properties,
    int count,
    String name,
    String runtime,
    boolean autoDeleteOnStop,
    boolean staticServices,
    Collection<String> nodes,
    Collection<ServiceRemoteInclusion> includes,
    Collection<ServiceTemplate> templates,
    Collection<ServiceDeployment> deployments,
    Collection<String> groups,
    Collection<String> deletedFilesAfterStop,
    ProcessConfiguration processConfiguration,
    int startPort,
    String javaCommand
  ) {
    Collection<ServiceInfoSnapshot> serviceInfoSnapshots = new ArrayList<>(count);

    Collection<ServiceTemplate> temps =
      properties.containsKey("templates") ? Arrays.asList(ServiceTemplate.parseArray(properties.get("templates")))
        : templates;
    Collection<ServiceDeployment> deploy =
      properties.containsKey("deployments") ? Arrays.stream(ServiceTemplate.parseArray(properties.get("deployments")))
        .map(serviceTemplate -> new ServiceDeployment(serviceTemplate, new ArrayList<>()))
        .collect(Collectors.toList()) : deployments;

    Integer finalStartPort = Ints.tryParse(properties.getOrDefault("port", String.valueOf(startPort)));
    if (finalStartPort == null) {
      finalStartPort = processConfiguration.getEnvironment().getDefaultStartPort();
    }

    for (int i = 0; i < count; i++) {
      ServiceInfoSnapshot serviceInfoSnapshot = CloudNetDriver.getInstance().getCloudServiceFactory()
        .createCloudService(new ServiceTask(
          includes,
          temps,
          deploy,
          properties.getOrDefault("name", name),
          properties.getOrDefault("runtime", runtime),
          properties.getOrDefault("autoDeleteOnStop", String.valueOf(autoDeleteOnStop)).equalsIgnoreCase("true"),
          properties.getOrDefault("static", String.valueOf(staticServices)).equalsIgnoreCase("true"),
          properties.containsKey("node") ? Arrays.asList(properties.get("node").split(";")) : nodes,
          properties.containsKey("groups") ? Arrays.asList(properties.get("groups").split(";")) : groups,
          properties.containsKey("deletedFilesAfterStop") ? Arrays
            .asList(properties.get("deletedFilesAfterStop").split(";")) : deletedFilesAfterStop,
          new ProcessConfiguration(
            processConfiguration.getEnvironment(),
            properties.containsKey("memory") && Ints.tryParse(properties.get("memory")) != null ?
              Integer.parseInt(properties.get("memory")) : processConfiguration.getMaxHeapMemorySize(),
            new ArrayList<>(properties.containsKey("jvmOptions") ?
              Arrays.asList(properties.get("jvmOptions").split(";")) :
              processConfiguration.getJvmOptions()),
            new ArrayList<>(properties.containsKey("processParameters") ?
              Arrays.asList(properties.get("processParameters").split(";")) :
              processConfiguration.getProcessParameters())
          ),
          finalStartPort,
          0,
          properties.getOrDefault("javaCommand", javaCommand)
        ));

      if (serviceInfoSnapshot != null) {
        serviceInfoSnapshots.add(serviceInfoSnapshot);
      }
    }

    return serviceInfoSnapshots;
  }

}
