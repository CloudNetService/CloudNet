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

package de.dytanic.cloudnet.ext.bridge.node.command;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.WildcardUtil;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.BridgeConfiguration;
import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import de.dytanic.cloudnet.ext.bridge.BridgeConstants;
import de.dytanic.cloudnet.ext.bridge.ProxyFallbackConfiguration;
import de.dytanic.cloudnet.ext.bridge.node.CloudNetBridgeModule;
import java.util.stream.Collectors;

public final class CommandBridge extends SubCommandHandler {

  public CommandBridge(CloudNetBridgeModule bridgeModule) {
    super(
      SubCommandBuilder.create()
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            BridgeConfiguration bridgeConfiguration = bridgeModule.reloadConfig()
              .get("config", BridgeConfiguration.TYPE);
            bridgeModule.setBridgeConfiguration(bridgeConfiguration);

            CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
              BridgeConstants.BRIDGE_CUSTOM_CHANNEL_MESSAGING_CHANNEL,
              "update_bridge_configuration",
              new JsonDocument("bridgeConfiguration", bridgeConfiguration)
            );

            sender.sendMessage(LanguageManager.getMessage("module-bridge-command-bridge-execute-success"));
          },
          SubCommand::onlyConsole,
          anyStringIgnoreCase("reload", "rl")
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            String targetGroup = (String) args.argument("targetGroup").get();

            BridgeConfiguration bridgeConfiguration = bridgeModule.getBridgeConfiguration();
            ProxyFallbackConfiguration fallbackConfiguration = bridgeModule
              .createDefaultFallbackConfiguration(targetGroup);
            bridgeConfiguration.getBungeeFallbackConfigurations().add(fallbackConfiguration);

            BridgeConfigurationProvider.update(bridgeConfiguration);

            sender.sendMessage(LanguageManager.getMessage("module-bridge-command-create-entry-success"));
          },
          anyStringIgnoreCase("create", "new"),
          exactStringIgnoreCase("entry"),
          dynamicString(
            "targetGroup",
            LanguageManager.getMessage("module-bridge-command-create-entry-group-not-found"),
            name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
            () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
              .map(GroupConfiguration::getName)
              .collect(Collectors.toList())
          )
        )

        .prefix(exactStringIgnoreCase("task"))
        .prefix(dynamicString(
          "name",
          LanguageManager.getMessage("command-tasks-task-not-found"),
          name -> WildcardUtil.anyMatch(
            CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks(),
            name
          ),
          () -> CloudNet.getInstance().getServiceTaskProvider().getPermanentServiceTasks()
            .stream()
            .filter(serviceTask -> serviceTask.getProcessConfiguration().getEnvironment().isMinecraftServer())
            .map(ServiceTask::getName)
            .collect(Collectors.toList())
        ))
        .prefix(exactStringIgnoreCase("set"))
        .applyHandler(CommandBridge::handleTaskSetCommands)
        .removeLastPrefix()

        .clearAll()

        .getSubCommands(),
      "bridge"
    );

    this.usage = "bridge | task";
    this.permission = "cloudnet.command.bridge";
    this.prefix = "cloudnet-bridge";
    this.description = LanguageManager.getMessage("module-bridge-command-bridge-description");
  }


  private static void handleTaskSetCommands(SubCommandBuilder builder) {
    builder
      .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
        ServiceTask serviceTask = CloudNetDriver.getInstance().getServiceTaskProvider()
          .getServiceTask(args.argument(1).toString());
        if (serviceTask == null) {
          // uses tasks command because they should exists
          sender.sendMessage(LanguageManager.getMessage("command-tasks-task-not-found"));
          return;
        }
        // checks if it is a Mineraft environment
        if (!serviceTask.getProcessConfiguration().getEnvironment().isMinecraftServer()) {
          // uses tasks command because they should exists
          sender.sendMessage(LanguageManager.getMessage("module-bridge-command-task-execute-no-java-server")
            .replace("%name%", serviceTask.getName()));
          return;

        }
        CloudNet.getInstance().getServiceTaskProvider().addPermanentServiceTask(serviceTask);

        // uses tasks command because they should exists
        sender.sendMessage(LanguageManager.getMessage("command-tasks-set-property-success")
          .replace("%property%", (String) args.argument(3))
          .replace("%name%", serviceTask.getName())
          .replace("%value%", String.valueOf(args.argument(4)))
        );
      })

      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          ServiceTask serviceTask = CloudNetDriver.getInstance().getServiceTaskProvider()
            .getServiceTask(args.argument(1).toString());
          if (serviceTask == null || !serviceTask.getProcessConfiguration().getEnvironment().isMinecraftServer()) {
            return;
          }
          String permission = (String) args.argument(4);
          // checks if the permission is "null" if so insert null
          if (permission.equalsIgnoreCase("null")) {
            serviceTask.getProperties().appendNull("requiredPermission");
          } else {
            serviceTask.getProperties().append("requiredPermission", permission);
          }
        },
        exactStringIgnoreCase("requiredPermission"),
        dynamicString("requiredPermission")
      )
      .removeLastPostHandler();
  }
}
