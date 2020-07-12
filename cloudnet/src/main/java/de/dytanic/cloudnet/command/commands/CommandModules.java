package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.sub.SubCommandBuilder;
import de.dytanic.cloudnet.command.sub.SubCommandHandler;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleConfiguration;
import de.dytanic.cloudnet.driver.module.ModuleId;
import de.dytanic.cloudnet.driver.module.dependency.ModuleDependency;
import de.dytanic.cloudnet.driver.module.dependency.ModuleDependencyType;
import de.dytanic.cloudnet.driver.module.repository.ModuleRepository;
import de.dytanic.cloudnet.driver.module.repository.RepositoryModuleInfo;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.anyStringIgnoreCase;
import static de.dytanic.cloudnet.command.sub.SubCommandArgumentTypes.dynamicString;

public class CommandModules extends SubCommandHandler {

    public CommandModules() {
        super(SubCommandBuilder.create()

                .prefix(anyStringIgnoreCase("list", "l"))
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            Collection<IModuleWrapper> modules = CloudNetDriver.getInstance().getModuleProvider().getModules();

                            for (IModuleWrapper wrapper : modules) {
                                if (matchesProperties(wrapper.getModuleConfiguration().getModuleId(), properties)) {
                                    displayInstalledModuleInfo(sender, wrapper);
                                }
                            }
                        },
                        subCommand -> subCommand.enableProperties().appendUsage("| group=<group> name=<name> version=<version>"),
                        anyStringIgnoreCase("installed", "local")
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ModuleRepository repository = CloudNetDriver.getInstance().getModuleProvider().getModuleRepository();
                            if (!repository.isReachable()) {
                                sender.sendMessage(LanguageManager.getMessage("command-modules-repository-not-available").replace("%url%", repository.getBaseURL()));
                                return;
                            }

                            Collection<RepositoryModuleInfo> modules = repository.getAvailableModules();
                            for (RepositoryModuleInfo moduleInfo : modules) {
                                if (!CloudNetDriver.getInstance().getModuleProvider().getModuleInstaller().isModuleInstalled(moduleInfo.getModuleId()) &&
                                        matchesProperties(moduleInfo.getModuleId(), properties)) {
                                    displayRemoteModuleInfo(sender, moduleInfo);
                                }
                            }
                        },
                        subCommand -> subCommand.enableProperties().appendUsage("| group=<group> name=<name> version=<version>"),
                        anyStringIgnoreCase("installable", "repo", "repository")
                )
                .removeLastPrefix()

                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ModuleId id = new ModuleId((String) args.argument("group").get(), (String) args.argument("name").get());
                            RepositoryModuleInfo info = CloudNetDriver.getInstance().getModuleProvider().getModuleRepository().getRepositoryModuleInfo(id);
                            if (info == null) {
                                sender.sendMessage(LanguageManager.getMessage("command-modules-repository-module-not-found").replace("%id%", id.toString()));
                                return;
                            }

                            if (CloudNetDriver.getInstance().getModuleProvider().getModuleInstaller().isModuleInstalled(id)) {
                                sender.sendMessage(LanguageManager.getMessage("command-modules-repository-module-already-installed").replace("%id%", id.toString()));
                                return;
                            }

                            try {
                                if (!CloudNetDriver.getInstance().getModuleProvider().getModuleInstaller().installModule(info, true)) {
                                    sender.sendMessage(LanguageManager.getMessage("command-modules-install-failed").replace("%id%", id.toString()));
                                    return;
                                }

                                sender.sendMessage(LanguageManager.getMessage("command-modules-install-success").replace("%id%", id.toString()));
                            } catch (IOException exception) {
                                sender.sendMessage(LanguageManager.getMessage("command-modules-install-error").replace("%id%", id.toString()).replace("%error%", exception.getLocalizedMessage()));
                            }
                        },
                        anyStringIgnoreCase("install", "add", "a", "i"),
                        dynamicString(
                                "group",
                                () -> CloudNetDriver.getInstance().getModuleProvider().getModuleRepository().getAvailableModules().stream()
                                        .map(RepositoryModuleInfo::getModuleId)
                                        .filter(moduleId -> !CloudNetDriver.getInstance().getModuleProvider().getModuleInstaller().isModuleInstalled(moduleId))
                                        .map(ModuleId::getGroup)
                                        .distinct()
                                        .collect(Collectors.toList())
                        ),
                        dynamicString(
                                "name",
                                () -> CloudNetDriver.getInstance().getModuleProvider().getModuleRepository().getAvailableModules().stream()
                                        .map(RepositoryModuleInfo::getModuleId)
                                        .filter(moduleId -> !CloudNetDriver.getInstance().getModuleProvider().getModuleInstaller().isModuleInstalled(moduleId))
                                        .map(ModuleId::getName)
                                        .distinct()
                                        .collect(Collectors.toList())
                        )
                )
                .generateCommand(
                        (subCommand, sender, command, args, commandLine, properties, internalProperties) -> {
                            ModuleId id = new ModuleId((String) args.argument("group").get(), (String) args.argument("name").get());
                            Optional<IModuleWrapper> module = CloudNetDriver.getInstance().getModuleProvider().getModules().stream()
                                    .filter(wrapper -> wrapper.getModuleConfiguration().getModuleId().equalsIgnoreVersion(id))
                                    .findFirst();
                            if (!module.isPresent()) {
                                sender.sendMessage(LanguageManager.getMessage("command-modules-repository-module-not-installed").replace("%id%", id.toString()));
                                return;
                            }

                            try {
                                CloudNetDriver.getInstance().getModuleProvider().getModuleInstaller().uninstallModule(id);

                                sender.sendMessage(LanguageManager.getMessage("command-modules-uninstall-success").replace("%id%", id.toString()));
                            } catch (IOException exception) {
                                sender.sendMessage(LanguageManager.getMessage("command-modules-uninstall-error").replace("%id%", id.toString()).replace("%error%", exception.getLocalizedMessage()));
                            }
                        },
                        anyStringIgnoreCase("uninstall", "remove", "r", "u"),
                        dynamicString(
                                "group",
                                () -> CloudNetDriver.getInstance().getModuleProvider().getModules().stream()
                                        .map(IModuleWrapper::getModuleConfiguration)
                                        .map(ModuleConfiguration::getGroup)
                                        .distinct()
                                        .collect(Collectors.toList())
                        ),
                        dynamicString(
                                "name",
                                () -> CloudNetDriver.getInstance().getModuleProvider().getModules().stream()
                                        .map(IModuleWrapper::getModuleConfiguration)
                                        .map(ModuleConfiguration::getName)
                                        .distinct()
                                        .collect(Collectors.toList())
                        )
                )

                .getSubCommands(), "modules", "module"
        );

        super.prefix = "cloudnet";
        super.permission = "cloudnet.command." + names[0];
        super.description = LanguageManager.getMessage("command-description-" + names[0]);
    }

    private static boolean matchesProperties(ModuleId moduleId, Properties properties) {
        if (properties.containsKey("group") && !moduleId.getGroup().contains(properties.get("group"))) {
            return false;
        }
        if (properties.containsKey("name") && !moduleId.getName().contains(properties.get("name"))) {
            return false;
        }
        return !properties.containsKey("version") || moduleId.getVersion().contains(properties.get("version"));
    }

    private static void displayRemoteModuleInfo(ICommandSender sender, RepositoryModuleInfo moduleInfo) {
        List<String> messages = new ArrayList<>();

        messages.add("Module: " + moduleInfo.getModuleId().getGroup() + ":" + moduleInfo.getModuleId().getName() + ":" + moduleInfo.getModuleId().getVersion());

        messages.add("* Runs on this CloudNet installation: " +
                (moduleInfo.getRequiredCloudNetVersion() == null || CommandModules.class.getPackage().getImplementationVersion().equals(moduleInfo.getRequiredCloudNetVersion()))
        );

        if (moduleInfo.getDescription() != null && !moduleInfo.getDescription().isEmpty()) {
            messages.add("* Description: " + moduleInfo.getDescription());
        }

        if (moduleInfo.getAuthors() != null && moduleInfo.getAuthors().length != 0) {
            messages.add("* Authors: " + String.join(", ", moduleInfo.getAuthors()));
        }

        if (moduleInfo.getWebsite() != null && !moduleInfo.getWebsite().isEmpty()) {
            messages.add("* Website: " + moduleInfo.getWebsite());
        }

        if (moduleInfo.getSourceUrl() != null && !moduleInfo.getSourceUrl().isEmpty()) {
            messages.add("* SourceCode: " + moduleInfo.getSourceUrl());
        }

        if (moduleInfo.getSupportUrl() != null && !moduleInfo.getSupportUrl().isEmpty()) {
            messages.add("* Support: " + moduleInfo.getSupportUrl());
        }

        if (moduleInfo.getDepends() != null && moduleInfo.getDepends().length != 0) {
            messages.add("* Required modules: " + Arrays.stream(moduleInfo.getDepends()).map(ModuleId::toString).collect(Collectors.joining(", ")));
        }

        messages.add(" ");

        sender.sendMessage(messages.toArray(new String[0]));
    }

    private static void displayInstalledModuleInfo(ICommandSender sender, IModuleWrapper moduleWrapper) {
        List<String> messages = new ArrayList<>();

        messages.add("* Module: " + moduleWrapper.getModuleConfiguration().getModuleId() + " by " + moduleWrapper.getModuleConfiguration().getAuthor());

        messages.add("Health status: " + moduleWrapper.getModuleLifeCycle().name());

        if (moduleWrapper.getModuleConfiguration().getAuthor() != null) {
            messages.add("Author: " + moduleWrapper.getModuleConfiguration().getAuthor());
        }

        if (moduleWrapper.getModuleConfiguration().getWebsite() != null) {
            messages.add("Website: " + moduleWrapper.getModuleConfiguration().getWebsite());
        }

        if (moduleWrapper.getModuleConfiguration().getDescription() != null) {
            messages.add("Description: " + moduleWrapper.getModuleConfiguration().getDescription());
        }

        if (moduleWrapper.getModuleConfiguration().getDependencies() != null) {
            messages.add(" ");
            messages.add("Dependencies: ");
            for (ModuleDependency moduleDependency : moduleWrapper.getModuleConfiguration().getDependencies()) {
                String dependencyMessage = " - Dependency: " + moduleDependency.asModuleId();
                if (moduleDependency.getType() != ModuleDependencyType.MODULE) {
                    dependencyMessage += " - " + (moduleDependency.getUrl() != null ? "Url: " + moduleDependency.getUrl() : "Repository: " + moduleDependency.getRepo());
                }
                messages.add(dependencyMessage);
            }
        }

        if (moduleWrapper.getModuleConfiguration().getProperties() != null) {
            messages.add(" ");
            messages.add("Properties: ");
            messages.addAll(Arrays.asList(moduleWrapper.getModuleConfiguration().getProperties().toPrettyJson().split("\n")));
        }

        messages.add(" ");

        sender.sendMessage(messages.toArray(new String[0]));
    }

    @Override
    protected void sendHelp(ICommandSender sender) {
        Collection<IModuleWrapper> modules = CloudNetDriver.getInstance().getModuleProvider().getModules();
        sender.sendMessage("Modules (" + modules.size() + ")" + modules.stream()
                .map(moduleWrapper -> moduleWrapper.getModule().getName()).collect(Collectors.joining(", ", ": ", "")));
        super.sendHelp(sender);
    }

}
