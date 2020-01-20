package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.sub.SubCommand;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import de.dytanic.cloudnet.driver.service.ServiceDeployment;
import de.dytanic.cloudnet.driver.service.ServiceRemoteInclusion;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.service.EmptyGroupConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.*;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;

public class CommandServiceConfigurationBase extends SubCommandHandler {
    protected CommandServiceConfigurationBase(Collection<SubCommand> subCommands, String... names) {
        super(subCommands, names);
    }

    protected static void handleGeneralAddCommands(SubCommandBuilder builder, Function<Map<String, Object>, ServiceConfigurationBase> configurationBaseFunction,
                                                 Consumer<ServiceConfigurationBase> updateHandler) {
        builder
                .prefix(exactStringIgnoreCase("add"))

                .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> updateHandler.accept(configurationBaseFunction.apply(internalProperties)))

                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args.argument(4);
                            Collection<String> excludes = args.length() > 5 ? (Collection<String>) args.argument(5) : new ArrayList<>();

                            configuration.getDeployments().add(new ServiceDeployment(template, excludes));

                            sender.sendMessage(LanguageManager.getMessage("command-service-base-add-deployment-success"));
                        },
                        subCommand -> subCommand.setMinArgs(5).setMaxArgs(Integer.MAX_VALUE),
                        exactStringIgnoreCase("deployment"),
                        template("storage:prefix/name"),
                        collection("excludedFiles separated by \";\"")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args.argument(4);

                            configuration.getTemplates().add(template);

                            sender.sendMessage(LanguageManager.getMessage("command-service-base-add-template-success"));
                        },
                        exactStringIgnoreCase("template"),
                        template("storage:prefix/name")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            String url = (String) args.argument(4);
                            String target = (String) args.argument(5);

                            configuration.getIncludes().add(new ServiceRemoteInclusion(url, target));

                            sender.sendMessage(LanguageManager.getMessage("command-service-base-add-inclusion-success"));
                        },
                        exactStringIgnoreCase("inclusion"),
                        url("url"),
                        dynamicString("targetPath")
                )

                .removeLastPrefix()
                .removeLastPostHandler();
    }

    protected static void handleGeneralRemoveCommands(SubCommandBuilder builder, Function<Map<String, Object>, ServiceConfigurationBase> configurationBaseFunction,
                                                      Consumer<ServiceConfigurationBase> updateHandler) {
        builder
                .prefix(exactStringIgnoreCase("remove"))

                .postExecute((subCommand, sender, command, args, commandLine, properties, internalProperties) -> updateHandler.accept(configurationBaseFunction.apply(internalProperties)))

                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args.argument(4);

                            configuration.getDeployments().removeAll(configuration.getDeployments().stream()
                                    .filter(deployment -> deployment.getTemplate().equals(template))
                                    .collect(Collectors.toList())
                            );

                            sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-deployment-success"));
                        },
                        exactStringIgnoreCase("deployment"),
                        template("storage:prefix/name")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            ServiceTemplate template = (ServiceTemplate) args.argument(4);

                            configuration.getTemplates().removeAll(configuration.getTemplates().stream()
                                    .filter(serviceTemplate -> serviceTemplate.equals(template))
                                    .collect(Collectors.toList())
                            );

                            sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-template-success"));
                        },
                        exactStringIgnoreCase("template"),
                        template("storage:prefix/name")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ServiceConfigurationBase configuration = configurationBaseFunction.apply(internalProperties);
                            String url = (String) args.argument(4);
                            String target = (String) args.argument(5);

                            configuration.getIncludes().removeAll(configuration.getIncludes().stream()
                                    .filter(inclusion -> inclusion.getDestination().equalsIgnoreCase(target))
                                    .filter(inclusion -> inclusion.getUrl().equalsIgnoreCase(url))
                                    .collect(Collectors.toList())
                            );

                            sender.sendMessage(LanguageManager.getMessage("command-service-base-remove-inclusion-success"));
                        },
                        exactStringIgnoreCase("inclusion"),
                        url("url"),
                        dynamicString("targetPath")
                )

                .removeLastPrefix()
                .removeLastPostHandler();
    }

    protected static void applyDisplayMessagesForServiceConfigurationBase(Collection<String> messages, ServiceConfigurationBase configurationBase) {
        messages.add("* Includes:");

        for (ServiceRemoteInclusion inclusion : configurationBase.getIncludes()) {
            messages.add("- " + inclusion.getUrl() + " => " + inclusion.getDestination());
        }

        messages.add(" ");
        messages.add("* Templates:");

        for (ServiceTemplate template : configurationBase.getTemplates()) {
            messages.add("- " + template.getStorage() + ":" + template.getTemplatePath());
        }

        messages.add(" ");
        messages.add("* Deployments:");

        for (ServiceDeployment deployment : configurationBase.getDeployments()) {
            messages.add("- ");
            messages.add("Template:  " + deployment.getTemplate().getStorage() + ":" + deployment.getTemplate().getTemplatePath());
            messages.add("Excludes: " + deployment.getExcludes());
        }

        messages.add(" ");

        messages.addAll(Arrays.asList(configurationBase.getProperties().toPrettyJson().split("\n")));
        messages.add(" ");
    }

    protected static void createEmptyGroupConfiguration(String name) {
        CloudNet.getInstance().getCloudServiceManager().addGroupConfiguration(new EmptyGroupConfiguration(name));
    }

}
