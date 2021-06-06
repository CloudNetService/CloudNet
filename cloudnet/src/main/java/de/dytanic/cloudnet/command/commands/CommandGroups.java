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
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactEnum;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class CommandGroups extends CommandServiceConfigurationBase {

  public CommandGroups() {
    super(
      SubCommandBuilder.create()

        .applyHandler(CommandGroups::handleDeleteCommands)
        .applyHandler(CommandGroups::handleAddCommands)

        .generateCommand((subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          CloudNet.getInstance().getGroupConfigurationProvider().reload();
          sender.sendMessage(LanguageManager.getMessage("command-groups-reload-success"));
        }, anyStringIgnoreCase("reload", "rl"))

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            sender.sendMessage("- Groups", " ");

            for (GroupConfiguration groupConfiguration : CloudNet.getInstance().getGroupConfigurationProvider()
              .getGroupConfigurations()) {
              if (properties.containsKey("name") &&
                !groupConfiguration.getName().toLowerCase().contains(properties.get("name").toLowerCase())) {
                continue;
              }

              sender.sendMessage("- " + groupConfiguration.getName());
            }
          },
          subCommand -> subCommand.enableProperties().appendUsage("| name=NAME"),
          exactStringIgnoreCase("list")
        )

        .prefix(exactStringIgnoreCase("group"))
        .prefix(dynamicString(
          "name",
          LanguageManager.getMessage("command-service-base-group-not-found"),
          name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
          () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations()
            .stream()
            .map(GroupConfiguration::getName)
            .collect(Collectors.toList())
        ))

        .preExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) ->
          internalProperties.put("group",
            CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfiguration((String) args.argument(1)))
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> displayGroup(sender,
            (GroupConfiguration) internalProperties.get("group")))

        .applyHandler(builder -> handleGeneralAddCommands(
          builder,
          internalProperties -> new ServiceConfigurationBase[]{
            (ServiceConfigurationBase) internalProperties.get("group")},
          serviceConfigurationBase -> CloudNet.getInstance().getGroupConfigurationProvider()
            .addGroupConfiguration((GroupConfiguration) serviceConfigurationBase)
        ))
        .applyHandler(builder -> handleGeneralRemoveCommands(
          builder,
          internalProperties -> new ServiceConfigurationBase[]{
            (ServiceConfigurationBase) internalProperties.get("group")},
          serviceConfigurationBase -> CloudNet.getInstance().getGroupConfigurationProvider()
            .addGroupConfiguration((GroupConfiguration) serviceConfigurationBase)
        ))

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            GroupConfiguration configuration = (GroupConfiguration) internalProperties.get("group");
            ServiceEnvironmentType environment = (ServiceEnvironmentType) args.argument(QuestionAnswerTypeEnum.class)
              .get();
            if (configuration.getTargetEnvironments().contains(environment)) {
              sender.sendMessage(LanguageManager.getMessage("command-groups-add-environment-already-existing"));
              return;
            }
            configuration.getTargetEnvironments().add(environment);
            CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(configuration);
            sender.sendMessage(LanguageManager.getMessage("command-groups-add-environment-success"));
          },
          exactStringIgnoreCase("add"),
          anyStringIgnoreCase("environment", "env"),
          exactEnum(ServiceEnvironmentType.class)
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            GroupConfiguration configuration = (GroupConfiguration) internalProperties.get("group");
            ServiceEnvironmentType environment = (ServiceEnvironmentType) args.argument(QuestionAnswerTypeEnum.class)
              .get();
            if (!configuration.getTargetEnvironments().contains(environment)) {
              sender.sendMessage(LanguageManager.getMessage("command-groups-remove-environment-not-found"));
              return;
            }
            configuration.getTargetEnvironments().remove(environment);
            CloudNet.getInstance().getGroupConfigurationProvider().addGroupConfiguration(configuration);
            sender.sendMessage(LanguageManager.getMessage("command-groups-remove-environment-success"));
          },
          exactStringIgnoreCase("remove"),
          anyStringIgnoreCase("environment", "env"),
          exactEnum(ServiceEnvironmentType.class)
        )

        .getSubCommands(),
      "groups"
    );

    super.prefix = "cloudnet";
    super.permission = "cloudnet.command.groups";
    super.description = LanguageManager.getMessage("command-description-groups");
  }

  private static void handleAddCommands(SubCommandBuilder builder) {
    builder
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          createEmptyGroupConfiguration((String) args.argument("name").get());
          sender.sendMessage(LanguageManager.getMessage("command-service-base-create-group"));
        },
        exactStringIgnoreCase("create"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-groups-group-already-existing"),
          name -> !CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name)
        )
      );
  }

  private static void handleDeleteCommands(SubCommandBuilder builder) {
    builder
      .generateCommand(
        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
          CloudNet.getInstance().getGroupConfigurationProvider()
            .removeGroupConfiguration((String) args.argument("name").get());
          sender.sendMessage(LanguageManager.getMessage("command-groups-delete-group"));
        },
        exactStringIgnoreCase("delete"),
        dynamicString(
          "name",
          LanguageManager.getMessage("command-service-base-group-not-found"),
          name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
          () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
            .map(GroupConfiguration::getName)
            .collect(Collectors.toList())
        )
      );
  }

  private static void displayGroup(ICommandSender sender, GroupConfiguration groupConfiguration) {

    Collection<String> messages = new ArrayList<>(Arrays.asList(
      " ",
      "* Name: " + groupConfiguration.getName(),
      " ",
      "* Environments: " + groupConfiguration.getTargetEnvironments()
    ));

    applyDisplayMessagesForServiceConfigurationBase(messages, groupConfiguration);

    sender.sendMessage(messages.toArray(new String[0]));
  }

}
