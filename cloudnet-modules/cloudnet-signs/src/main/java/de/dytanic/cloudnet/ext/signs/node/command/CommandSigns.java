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

package de.dytanic.cloudnet.ext.signs.node.command;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactEnum;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfiguration;
import de.dytanic.cloudnet.ext.signs.configuration.SignConfigurationReaderAndWriter;
import de.dytanic.cloudnet.ext.signs.configuration.entry.SignConfigurationEntryType;
import de.dytanic.cloudnet.ext.signs.node.CloudNetSignsModule;
import java.util.stream.Collectors;

public final class CommandSigns extends SubCommandHandler {

  public CommandSigns() {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            CloudNetSignsModule.getInstance().setSignConfiguration(SignConfigurationReaderAndWriter.read(
              CloudNetSignsModule.getInstance().getConfigurationFilePath()
            ));

            CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
              SignConstants.SIGN_CHANNEL_NAME,
              SignConstants.SIGN_CHANNEL_UPDATE_SIGN_CONFIGURATION,
              new JsonDocument("signConfiguration", CloudNetSignsModule.getInstance().getSignConfiguration())
            );

            sender.sendMessage(LanguageManager.getMessage("module-signs-command-reload-success"));
          },
          anyStringIgnoreCase("reload", "rl")
        )

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            String targetGroup = (String) args.argument("targetGroup").get();
            SignConfigurationEntryType signConfigurationEntryType = (SignConfigurationEntryType) args
              .argument(QuestionAnswerTypeEnum.class).get();

            SignConfiguration signConfiguration = CloudNetSignsModule.getInstance().getSignConfiguration();

            signConfiguration.getConfigurations().add(signConfigurationEntryType.createEntry(targetGroup));
            SignConfigurationReaderAndWriter
              .write(signConfiguration, CloudNetSignsModule.getInstance().getConfigurationFilePath());

            sender.sendMessage(LanguageManager.getMessage("module-signs-command-create-entry-success"));
          },
          anyStringIgnoreCase("create", "new"),
          exactStringIgnoreCase("entry"),
          dynamicString(
            "targetGroup",
            LanguageManager.getMessage("module-signs-command-create-entry-group-not-found"),
            name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
            () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
              .map(GroupConfiguration::getName)
              .collect(Collectors.toList())
          ),
          exactEnum(SignConfigurationEntryType.class)
        )

        .getSubCommands(),
      "signs", "sign", "cloud-signs"
    );

    this.permission = "cloudnet.command.signs";
    this.prefix = "cloudnet-signs";
    this.description = LanguageManager.getMessage("module-signs-command-signs-description");
  }
}
