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

package eu.cloudnetservice.cloudnet.ext.npcs.node.command;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.exactStringIgnoreCase;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.npcs.node.CloudNetNPCModule;
import java.util.stream.Collectors;

public class CommandNPC extends SubCommandHandler {

  public CommandNPC(CloudNetNPCModule npcModule) {
    super(
      SubCommandBuilder.create()

        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            String targetGroup = (String) args.argument("targetGroup").get();

            NPCConfiguration npcConfiguration = npcModule.getNPCConfiguration();

            npcConfiguration.getConfigurations().add(new NPCConfigurationEntry(targetGroup));
            npcModule.saveNPCConfiguration();

            sender.sendMessage(LanguageManager.getMessage("module-npcs-command-create-entry-success"));
          },
          anyStringIgnoreCase("create", "new"),
          exactStringIgnoreCase("entry"),
          dynamicString(
            "targetGroup",
            LanguageManager.getMessage("module-npcs-command-create-entry-group-not-found"),
            name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
            () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
              .map(GroupConfiguration::getName)
              .collect(Collectors.toList())
          )
        )
        .generateCommand(
          (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
            NPCConfiguration.sendNPCConfigurationUpdate(npcModule.readConfiguration());
            sender.sendMessage(LanguageManager.getMessage("module-npcs-command-reload-success"));
          },
          anyStringIgnoreCase("reload", "rl")
        )
        .getSubCommands(),
      "npc", "npcs", "cloud-npc"
    );

    this.permission = "cloudnet.command.npcs";
    this.prefix = "cloudnet-npcs";
    this.description = LanguageManager.getMessage("module-npcs-command-npcs-description");
  }

}
