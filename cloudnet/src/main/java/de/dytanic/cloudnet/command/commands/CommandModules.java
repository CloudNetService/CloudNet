package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.command.ITabCompleter;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.module.ModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandModules extends CommandDefault implements ITabCompleter {

    public CommandModules() {
        super("modules", "module");
    }

    /**
     * An unsafe implementation for managing the module system
     * Use the "reload" for a save module reloading
     *
     * @param sender
     * @param command
     * @param args
     * @param commandLine
     * @param properties
     */
    @Deprecated
    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        ModuleProvider moduleProvider = CloudNetDriver.getInstance().getModuleProvider();
        Collection<ModuleWrapper> moduleWrappers = moduleProvider.getModules();

        if (args.length == 0) {
            sender.sendMessage(
                    "Modules(" + moduleWrappers.size() + "): " + Arrays.toString(Iterables.map(moduleWrappers, moduleWrapper -> moduleWrapper.getModule().getName()).toArray(new String[0])),
                    " ",
                    "modules list | group=<name> name=<name> version=<version>"
            );
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {
            for (ModuleWrapper wrapper : moduleWrappers) {
                if (properties.containsKey("group") && !wrapper.getModuleConfiguration().getGroup().contains(properties.get("group"))) {
                    continue;
                }
                if (properties.containsKey("name") && !wrapper.getModuleConfiguration().getName().contains(properties.get("name"))) {
                    continue;
                }
                if (properties.containsKey("version") && !wrapper.getModuleConfiguration().getVersion().contains(properties.get("version"))) {
                    continue;
                }

                this.displayModuleInfo(sender, wrapper);
            }
        }

        ModuleWrapper wrapper = CloudNetDriver.getInstance().getModuleProvider().getModule(args[0]);
        if (wrapper != null) {
            this.displayModuleInfo(sender, wrapper);
        }
    }

    private void displayModuleInfo(ICommandSender sender, ModuleWrapper moduleWrapper) {
        List<String> list = Iterables.newArrayList();

        list.add("* Module: " +
                moduleWrapper.getModuleConfiguration().getGroup() + ":" +
                moduleWrapper.getModuleConfiguration().getName() + ":" +
                moduleWrapper.getModuleConfiguration().getVersion()
        );

        list.add("* Health status: " + moduleWrapper.getModuleLifeCycle().name());

        if (moduleWrapper.getModuleConfiguration().getAuthor() != null) {
            list.add("* Author: " + moduleWrapper.getModuleConfiguration().getAuthor());
        }

        if (moduleWrapper.getModuleConfiguration().getWebsite() != null) {
            list.add("* Website: " + moduleWrapper.getModuleConfiguration().getWebsite());
        }

        if (moduleWrapper.getModuleConfiguration().getDescription() != null) {
            list.add("* Description: " + moduleWrapper.getModuleConfiguration().getDescription());
        }

        if (moduleWrapper.getModuleConfiguration().getDependencies() != null) {
            list.add(" ");
            list.add("* Dependencies: ");
            for (ModuleDependency moduleDependency : moduleWrapper.getModuleConfiguration().getDependencies()) {
                list.addAll(Arrays.asList(
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
            list.add(" ");
            list.add("* Properties: ");
            list.addAll(Arrays.asList(moduleWrapper.getModuleConfiguration().getProperties().toPrettyJson().split("\n")));
        }

        list.add(" ");

        sender.sendMessage(
                "Module: " +
                        moduleWrapper.getModuleConfiguration().getGroup() + ":" +
                        moduleWrapper.getModuleConfiguration().getName() + ":" +
                        moduleWrapper.getModuleConfiguration().getVersion(),
                "Author: " + moduleWrapper.getModuleConfiguration().getAuthor()
        );
        sender.sendMessage(list.toArray(new String[0]));
    }

    @Override
    public Collection<String> complete(String commandLine, String[] args, Properties properties) {
        if (args.length > 1)
            return null;
        Collection<String> response = CloudNetDriver.getInstance().getModuleProvider().getModules()
                .stream().map(moduleWrapper -> moduleWrapper.getModule().getName()).collect(Collectors.toList());
        response.add("list");
        return response;
    }
}
