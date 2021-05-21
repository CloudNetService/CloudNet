package eu.cloudnetservice.cloudnet.ext.signs.node.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeEnum;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.NodeSignsConfigurationHelper;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.SignConfigurationType;
import eu.cloudnetservice.cloudnet.ext.signs.node.util.SignPluginInclusion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;

public class CommandSign extends SubCommandHandler {

    public CommandSign(@NotNull SignManagement signManagement, @NotNull Path configurationPath) {
        super(SubCommandBuilder.create()
                // signs reload
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            signManagement.setSignsConfiguration(NodeSignsConfigurationHelper.read(configurationPath));
                            sender.sendMessage(LanguageManager.getMessage("module-signs-command-reload-success"));
                        },
                        anyStringIgnoreCase("rl", "reload")
                )
                // signs create entry <group>
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            String targetGroup = (String) args.argument("group").get();
                            SignConfigurationType type = (SignConfigurationType) args.argument(QuestionAnswerTypeEnum.class).get();

                            SignsConfiguration configuration = signManagement.getSignsConfiguration();
                            if (SignPluginInclusion.hasConfigurationEntry(Collections.singleton(targetGroup), configuration)) {
                                sender.sendMessage(LanguageManager.getMessage("module-signs-command-create-entry-already-there"));
                            } else {
                                configuration.getConfigurationEntries().add(type.createEntry(targetGroup));
                                signManagement.setSignsConfiguration(configuration);

                                sender.sendMessage(LanguageManager.getMessage("module-signs-command-create-entry-success"));
                            }
                        },
                        anyStringIgnoreCase("create", "new"),
                        exactStringIgnoreCase("entry"),
                        dynamicString(
                                "group",
                                LanguageManager.getMessage("module-signs-command-create-entry-group-not-found"),
                                name -> CloudNet.getInstance().getGroupConfigurationProvider().isGroupConfigurationPresent(name),
                                () -> CloudNet.getInstance().getGroupConfigurationProvider().getGroupConfigurations().stream()
                                        .map(GroupConfiguration::getName)
                                        .collect(Collectors.toList())
                        ),
                        exactEnum(SignConfigurationType.class)
                )
                .getSubCommands()
        );

        this.prefix = "cloudnet-signs";
        this.names = new String[]{"signs", "sign"};
        this.permission = "cloudnet.command.signs";
        this.description = LanguageManager.getMessage("module-signs-command-signs-description");
    }
}
