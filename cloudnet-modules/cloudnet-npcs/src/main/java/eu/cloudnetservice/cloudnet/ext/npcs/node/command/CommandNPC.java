package eu.cloudnetservice.cloudnet.ext.npcs.node.command;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfiguration;
import eu.cloudnetservice.cloudnet.ext.npcs.configuration.NPCConfigurationEntry;
import eu.cloudnetservice.cloudnet.ext.npcs.node.CloudNetNPCModule;

import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

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
                                    NPCConfiguration.sendNPCConfigurationUpdate(npcModule.getNPCConfiguration());
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
