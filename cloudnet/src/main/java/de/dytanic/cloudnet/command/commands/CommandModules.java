package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleId;
import de.dytanic.cloudnet.driver.module.dependency.ModuleDependency;
import de.dytanic.cloudnet.driver.module.repository.ModuleInstaller;
import de.dytanic.cloudnet.driver.module.repository.ModuleRepository;
import de.dytanic.cloudnet.driver.module.repository.RepositoryModuleInfo;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class CommandModules extends CommandDefault implements ITabCompleter {

    public CommandModules() {
        super("modules", "module");
    }

    @Deprecated
    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        IModuleProvider moduleProvider = super.getCloudNet().getModuleProvider();
        ModuleRepository moduleRepository = moduleProvider.getModuleRepository();
        ModuleInstaller moduleInstaller = moduleProvider.getModuleInstaller();

        Collection<IModuleWrapper> moduleWrappers = moduleProvider.getModules();

        if (args.length == 0) {
            sender.sendMessage(
                    "Modules (" + moduleWrappers.size() + "): " + Arrays.toString(moduleWrappers.stream()
                            .map(moduleWrapper -> moduleWrapper.getModule().getName()).toArray(String[]::new)),
                    " ",
                    "modules list installed | group=<name> name=<name> version=<version> author=<author>",
                    "modules list installable | group=<name> name=<name> version=<version> author=<author>",
                    "modules install <group> <name>",
                    "modules uninstall <group> <name>"
            );
            return;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("list")) {

            if (args[1].equalsIgnoreCase("installed")) {
                for (IModuleWrapper wrapper : moduleWrappers) {
                    if (this.matchesProperties(wrapper.getModuleConfiguration().getModuleId(), properties)) {
                        this.displayInstalledModuleInfo(sender, wrapper);
                    }
                }
            } else if (args[1].equalsIgnoreCase("installable")) {
                if (!moduleRepository.isReachable()) {
                    sender.sendMessage(LanguageManager.getMessage("command-modules-repository-not-available").replace("%url%", moduleRepository.getBaseURL()));
                    return;
                }

                Collection<RepositoryModuleInfo> moduleInfos = moduleRepository.getAvailableModules();
                for (RepositoryModuleInfo moduleInfo : moduleInfos) {
                    if (!moduleInstaller.isModuleInstalled(moduleInfo.getModuleId()) &&
                            this.matchesProperties(moduleInfo.getModuleId(), properties)) {
                        this.displayRemoteModuleInfo(sender, moduleInfo);
                    }
                }
            }

        } else if (args.length == 3 && args[0].equalsIgnoreCase("install")) {

            ModuleId moduleId = new ModuleId(args[1], args[2]);
            RepositoryModuleInfo moduleInfo = moduleRepository.loadRepositoryModuleInfo(moduleId);

            if (moduleInfo == null) {
                sender.sendMessage(LanguageManager.getMessage("command-modules-repository-module-not-found").replace("%id%", moduleId.toString()));
                return;
            }

            if (moduleInstaller.isModuleInstalled(moduleId)) {
                sender.sendMessage(LanguageManager.getMessage("command-modules-repository-module-already-installed").replace("%id%", moduleId.toString()));
                return;
            }

            try {
                if (!moduleInstaller.installModule(moduleInfo, true)) {
                    sender.sendMessage(LanguageManager.getMessage("command-modules-install-failed").replace("%id%", moduleId.toString()));
                    return;
                }

                sender.sendMessage(LanguageManager.getMessage("command-modules-install-success").replace("%id%", moduleId.toString()));
            } catch (IOException exception) {
                exception.printStackTrace();
            }

        } else if (args.length == 3 && args[0].equalsIgnoreCase("uninstall")) {

            ModuleId moduleId = new ModuleId(args[1], args[2]);

            if (!moduleInstaller.isModuleInstalled(moduleId)) {
                sender.sendMessage(LanguageManager.getMessage("command-modules-repository-module-not-installed").replace("%id%", moduleId.toString()));
                return;
            }

            try {
                moduleInstaller.uninstallModule(moduleId);

                sender.sendMessage(LanguageManager.getMessage("command-modules-uninstall-success").replace("%id%", moduleId.toString()));
            } catch (IOException exception) {
                exception.printStackTrace();
            }

        }
    }

    private boolean matchesProperties(ModuleId moduleId, Properties properties) {
        if (properties.containsKey("group") && !moduleId.getGroup().contains(properties.get("group"))) {
            return false;
        }
        if (properties.containsKey("name") && !moduleId.getName().contains(properties.get("name"))) {
            return false;
        }
        return !properties.containsKey("version") || moduleId.getVersion().contains(properties.get("version"));
    }

    private void displayRemoteModuleInfo(ICommandSender sender, RepositoryModuleInfo moduleInfo) {
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

    private void displayInstalledModuleInfo(ICommandSender sender, IModuleWrapper moduleWrapper) {
        List<String> messages = new ArrayList<>();

        messages.add("* Module: " +
                moduleWrapper.getModuleConfiguration().getGroup() + ":" +
                moduleWrapper.getModuleConfiguration().getName() + "-" +
                moduleWrapper.getModuleConfiguration().getVersion()
        );

        messages.add("* Health status: " + moduleWrapper.getModuleLifeCycle().name());

        if (moduleWrapper.getModuleConfiguration().getAuthor() != null) {
            messages.add("* Author: " + moduleWrapper.getModuleConfiguration().getAuthor());
        }

        if (moduleWrapper.getModuleConfiguration().getWebsite() != null) {
            messages.add("* Website: " + moduleWrapper.getModuleConfiguration().getWebsite());
        }

        if (moduleWrapper.getModuleConfiguration().getDescription() != null) {
            messages.add("* Description: " + moduleWrapper.getModuleConfiguration().getDescription());
        }

        if (moduleWrapper.getModuleConfiguration().getDependencies() != null) {
            messages.add(" ");
            messages.add("* Dependencies: ");
            for (ModuleDependency moduleDependency : moduleWrapper.getModuleConfiguration().getDependencies()) {
                messages.addAll(Arrays.asList(
                        "- ",
                        "Dependency: " + moduleDependency.getGroup() + ":" + moduleDependency.getName() + ":" + moduleDependency.getVersion(),
                        (
                                moduleDependency.getUrl() != null ?
                                        "Url: " + moduleDependency.getUrl()
                                        :
                                        "Repository: " + moduleDependency.getRepo()
                        )
                ));
            }
        }

        if (moduleWrapper.getModuleConfiguration().getProperties() != null) {
            messages.add(" ");
            messages.add("* Properties: ");
            messages.addAll(Arrays.asList(moduleWrapper.getModuleConfiguration().getProperties().toPrettyJson().split("\n")));
        }

        messages.add(" ");

        sender.sendMessage(
                "Module: " +
                        moduleWrapper.getModuleConfiguration().getGroup() + ":" +
                        moduleWrapper.getModuleConfiguration().getName() + ":" +
                        moduleWrapper.getModuleConfiguration().getVersion(),
                "Author: " + moduleWrapper.getModuleConfiguration().getAuthor()
        );
        sender.sendMessage(messages.toArray(new String[0]));
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        //todo
        return Collections.emptyList();
        /*if (args.length > 1)
            return null;
        Collection<String> response = super.getCloudNet().getModuleProvider().getModules()
                .stream().map(moduleWrapper -> moduleWrapper.getModule().getName()).collect(Collectors.toList());
        response.add("list");
        return response;*/
    }
}
